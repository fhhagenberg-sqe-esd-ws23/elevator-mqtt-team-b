package at.fhhagenberg.sqelevator;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.datatypes.BuildingInfo;
import at.fhhagenberg.sqelevator.datatypes.ElevatorInfo;
import at.fhhagenberg.sqelevator.datatypes.FloorInfo;
import at.fhhagenberg.sqelevator.datatypes.MqttTopics;

import at.fhhagenberg.sqelevator.exceptions.*;
import sqelevator.IElevator;
import at.fhhagenberg.sqelevator.MqttCallbacks.CallbackContext;
import at.fhhagenberg.sqelevator.MqttCallbacks.CommittedDirectionCb;
import at.fhhagenberg.sqelevator.MqttCallbacks.FloorServicesCb;
import at.fhhagenberg.sqelevator.MqttCallbacks.TargetFloorCb;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import java.rmi.Naming;


/**
 * The ElevatorMqttAdapter provides the proprietary plc as MQTT interface <br>
 * It periodically reads data from the PLC, saves them in an internal data struct
 * and publishes them via MQTT. <br>
 * It also subscribes to MQTT control topics and forwards the received data directly to the PLC.
 */
public class ElevatorMqttAdapter extends TimerTask {
    private MqttAsyncClient client;
    private MemoryPersistence persistence = new MemoryPersistence();

    private int qos;
    private Boolean connected = false;
    private long timeoutMs;
    private int controlUpdateInterval_ms;    

    private BuildingInfo building;
    private IElevator elevatorIface;
    private CallbackContext cbContext = new CallbackContext();
    private MqttActionListener committedDirectionCb = new CommittedDirectionCb();
    private MqttActionListener floorServicesCb = new FloorServicesCb();
    private MqttActionListener targetFloorCb = new TargetFloorCb();
    private Timer timer = new Timer();
    private final long rmiConnectTimeoutS = 10;


    /**
     * @param broker host of broker
     * @param clientId id of client
     * @param qos mqtt quality of service
     * @param timeoutMs mqtt timeout in ms
     * @param controlUpdateInterval_ms Update interval in which data is polled from the PLC [ms]
     */
    public ElevatorMqttAdapter(String broker, String clientId, int qos, long timeoutMs, int controlUpdateInterval_ms) {
        this.building = new BuildingInfo();

        this.cbContext.buildingInfo = this.building;
        this.cbContext.elevatorIface = this.elevatorIface;
        this.cbContext.adapter = this;


        this.qos = qos;
        this.timeoutMs = timeoutMs;
        this.controlUpdateInterval_ms = controlUpdateInterval_ms;
        try {
            this.client = new MqttAsyncClient(broker, clientId, persistence);
        } catch (MqttException exc) {
            throw new MqttError(formatMqttException(exc));
        }
    }
    public ElevatorMqttAdapter(String broker, String clientId, int qos, long timeoutMs) {
        this(broker, clientId, qos, timeoutMs, 250);
    }

    public static void main(String[] args) {

		try {
            String broker = "192.168.xxx.xxx";
			ElevatorMqttAdapter adapter = new ElevatorMqttAdapter(broker, "elevatorAdapter", 2, 1000);
            adapter.run();
		} catch (ElevatorError exc) {
			System.err.println("Caught ElevatorError while running elevator MQTT adapter: " + exc.toString());
		} catch (Exception exc) {
			System.err.println("Caught unhandled exception while running elevator MQTT adapter: " + exc.toString());
		}
	}

    /** 
     * Disconnects the MQTT client. 
    */
    protected void finalize() {
        try {
            disconnectFromBroker();
        } catch (MqttError exc) {
            System.err.println(exc.getMessage());
        }
    }

    /**
     * Connects the MQTT client to the broker.
     * @throws MqttError
    */
    public void connectToBroker() {
        if (connected) {
            return;
        }

        try {
            MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setCleanStart(false);

            IMqttToken token = client.connect(connOpts);
            token.waitForCompletion(timeoutMs);

            connected = true;            
        } catch (MqttException exc) {
            throw new MqttError(formatMqttException(exc));
        }        
    }

    /**
     * Disconnects from MQTT client
     * @throws MqttError
     */
    public void disconnectFromBroker() {
        if (!connected) {
            return;
        }

        try {            
            client.close();
            connected = false;            
        } catch (MqttException exc) {
            throw new MqttError("While closing connection a " + formatMqttException(exc));
        }        
    }

    /**
     * Connects to the elevator control via RMI.
     */
    public void connectToElevator() {
        try {
            this.elevatorIface = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");
        } catch(Exception e) {
            try {
                System.out.println("Failed to connect to elevator via RMI, retrying in " + String.valueOf(this.rmiConnectTimeoutS) + " seconds");
                TimeUnit.SECONDS.sleep(this.rmiConnectTimeoutS);
            } catch(InterruptedException exc) {
                throw new RmiError("While waiting for reconnect to RMI: " + exc.toString());
            }
            // try again
            connectToElevator();
        }
    }
    
    /**
     * Connects to broker, subscribes to all control topics,  
     * publishes all retained topics and runs the update loop. 
     * @throws MqttError     
     */
    public void run() {
        // do setup
        connectToElevator();
        connectToBroker();
        readStates();
        subscribeToController();
        publishRetainedTopics();

        // run update loop
        MqttUpdateTimerTask updateTimerTask = new MqttUpdateTimerTask(() -> { readStates(); publishState(); return null; });
        timer.schedule(updateTimerTask, 0, controlUpdateInterval_ms);
    }

    /**
     * Stops the publish loop.
    */
    public void stop() {
        timer.cancel();
    }

    /**
     * Subscribes to all control topics 
     * @throws MqttError
     */
    public void subscribeToController() {
        if (!client.isConnected()) {
            throw new MqttError("MQTT client must be connected before subscribing to topics");
        }
        try {
            for (int id = 0; id < building.getNumberOfElevators(); id++) {
                // the committed direction control
                client.subscribe(
                    MqttTopics.controlElevatorTopic + String.valueOf(id) + "/committedDirection", 
                    qos, 
                    (Object)cbContext, 
                    committedDirectionCb
                );

                // the target floor control
                client.subscribe(
                    MqttTopics.controlElevatorTopic + String.valueOf(id) + "/targetFloor", 
                    qos, 
                    (Object)cbContext, 
                    targetFloorCb
                );

                // the floor services control
                for (int num = 0; num < building.getNumberOfFloors(); num++) {
                    client.subscribe(
                        MqttTopics.controlElevatorTopic + String.valueOf(id) + "/floorService/" + String.valueOf(num),
                        qos, 
                        (Object)cbContext, 
                        floorServicesCb
                    );
                }
            }
        } catch (MqttException exc) {
            throw new MqttError("While subscribing to topics a " + formatMqttException(exc));
        }
    }

    /** 
     * Publishes all retained (static) topics 
     * @throws MqttError    
    */
    public void publishRetainedTopics() {
        if (!client.isConnected()) {
            throw new MqttError("MQTT client must be connected before publishing messages");
        }
        MqttMessage msg = new MqttMessage(null, qos, true, null);

        try {
            // number of elevators
            msg.setPayload(String.valueOf(building.getNumberOfElevators()).getBytes());
            IMqttToken token = client.publish(MqttTopics.infoTopic + "numberOfElevators", msg);
            token.waitForCompletion(timeoutMs);

            // number of floors
            msg.setPayload(String.valueOf(building.getNumberOfFloors()).getBytes());
            token = client.publish(MqttTopics.infoTopic + "numberOfFloors", msg);
            token.waitForCompletion(timeoutMs);

            // floor height in feet
            msg.setPayload(String.valueOf(building.getFloorHeight()).getBytes());
            token = client.publish(MqttTopics.infoTopic + "floorHeight/feet", msg);
            token.waitForCompletion(timeoutMs);

            // system clock tick
            msg.setPayload(String.valueOf(building.getClockTick()).getBytes());
            token = client.publish(MqttTopics.infoTopic + "systemClockTick", msg);
            token.waitForCompletion(timeoutMs);

            // maximum number of passengers for the elevator
            for (int id = 0; id < building.getNumberOfElevators(); id++) {
                msg.setPayload(String.valueOf(building.getElevator(id).maxPassengers).getBytes());
                token = client.publish(MqttTopics.infoElevatorTopic + String.valueOf(id) + "/maxPassengers", msg);
                token.waitForCompletion(timeoutMs);
            }
        } catch (MqttException exc) {
            throw new MqttError("While publishing retained topics a " + formatMqttException(exc));
        }

    }

    /** 
     * Publishes updates on all non retained MQTT topics if it has changed.
     * This method should be called in a loop.
     * @throws MqttError
     */
    public void publishState() {
        if (!client.isConnected()) {
            throw new MqttError("MQTT client must be connected before publishing messages");
        }
        MqttMessage msg = new MqttMessage(null, qos, false, null);
        try {
            // publish updates for all elevators
            for (int id = 0; id < building.getNumberOfElevators(); id++) {
                ElevatorInfo elevator = building.getElevator(id);

                // committedDirection
                msg.setPayload(String.valueOf(elevator.committedDirection).getBytes());
                IMqttToken token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/committedDirection", msg);
                token.waitForCompletion(timeoutMs);

                // current acceleration in feet per square second
                msg.setPayload(String.valueOf(elevator.acceleration).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/acceleration/feetPerSqSec", msg);
                token.waitForCompletion(timeoutMs);

                // floorButtons
                for (int num = 0; num < building.getNumberOfFloors(); num++) {
                    msg.setPayload(String.valueOf(elevator.floorButtons[num]).getBytes());
                    token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floorButton/" + String.valueOf(num), msg);
                    token.waitForCompletion(timeoutMs);
                }

                // door status
                msg.setPayload(String.valueOf(elevator.doorStatus).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/door", msg);
                token.waitForCompletion(timeoutMs);

                // current floor
                msg.setPayload(String.valueOf(elevator.floor).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floor", msg);
                token.waitForCompletion(timeoutMs);

                // current height in feet
                msg.setPayload(String.valueOf(elevator.height).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/height/feet", msg);
                token.waitForCompletion(timeoutMs);

                // current speed in feet per second
                msg.setPayload(String.valueOf(elevator.speed).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/speed/feetPerSec", msg);
                token.waitForCompletion(timeoutMs);

                // current load in lbs
                msg.setPayload(String.valueOf(elevator.load).getBytes());
                token = client.publish(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/load/lbs", msg);
                token.waitForCompletion(timeoutMs);

                // floor service
                for (int num = 0; num < building.getNumberOfFloors(); num++) {
                    msg.setPayload(String.valueOf(elevator.floorsService[num]).getBytes());
                    token = client.publish(MqttTopics.infoElevatorTopic + String.valueOf(id) + "/floorService/" + String.valueOf(num), msg);
                    token.waitForCompletion(timeoutMs);
                }

                // current floor target
                msg.setPayload(String.valueOf(elevator.targetFloor).getBytes());
                token = client.publish(MqttTopics.infoElevatorTopic + String.valueOf(id) + "/targetFloor", msg);
                token.waitForCompletion(timeoutMs);
            }

            // publish updates for all floors
            for (int num = 0; num < building.getNumberOfFloors(); num++) {
                FloorInfo floor = building.getFloor(num);

                // status button 'up'
                msg.setPayload(String.valueOf(floor.callUp).getBytes());
                IMqttToken token = client.publish(MqttTopics.statusFloorTopic + String.valueOf(floor.floorId) + "/button/up", msg);
                token.waitForCompletion(timeoutMs);

                // status button 'down'
                msg.setPayload(String.valueOf(floor.callDown).getBytes());
                token = client.publish(MqttTopics.statusFloorTopic + String.valueOf(floor.floorId) + "/button/down", msg);
                token.waitForCompletion(timeoutMs);
            }

        } catch (MqttException exc) {
            throw new MqttError("While publishing topics a " + formatMqttException(exc));
        }
    }

    /** 
     * Read the current state of all elevators and floors in the
     * building using the Elevator PLC.
    * */
    public void readStates() {
        try {
            building.populate(elevatorIface);        
        } catch(ControlError exc) {
            System.out.println("Lost RMI connection to elevator control, trying to reconnect...");
            connectToElevator();
            // read states again
            readStates();
        }            
}
    private String formatMqttException(MqttException exc) {
        String msg = "MQTT error occurred:\n\treason " + exc.getReasonCode();
        msg += "\tmsg " + exc.getMessage();
        msg += "\tloc " + exc.getLocalizedMessage();
        msg += "\tcause " + exc.getCause();
        msg += "\texcep " + exc;
        return msg;
    }    
}
