package at.fhhagenberg.sqelevator.Algorithm;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import at.fhhagenberg.sqelevator.MqttTopics;
import at.fhhagenberg.sqelevator.MqttUpdateTimerTask;
import at.fhhagenberg.sqelevator.exceptions.ElevatorError;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;
import at.fhhagenberg.sqelevator.exceptions.InternalError;

public class AlgoMqttClient implements MqttCallback {
    private MqttAsyncClient client;
    private MemoryPersistence persistence = new MemoryPersistence();

    private long timeoutMs;
    private int qos;
    private Boolean connected = false;

    private Timer timer = new Timer();
    private int targetUpdateInterval_ms;
    private boolean RmiConnected = false;

    private Building building;

    private int nrOfElevators = -1;
    private int nrOfFloors = -1;
    private Vector<Integer> maxNrPassengers = new Vector<Integer>();

    private Pattern digitRegex = Pattern.compile("\\d");

    AlgoMqttClient(String broker, String clientId, int qos, long timeoutMs, int targetUpdateInterval_ms) {
        this.timeoutMs = timeoutMs;
        this.qos = qos;
        this.targetUpdateInterval_ms = targetUpdateInterval_ms;
        try {
            this.client = new MqttAsyncClient(broker, clientId, persistence);
        } catch (MqttException exc) {
            throw new MqttError(formatMqttException(exc));
        }
    }

    AlgoMqttClient(String broker, String clientId, int qos, long timeoutMs) {
        this(broker, clientId, qos, timeoutMs, 250);
    }

     public static void main(String[] args) {
		try {
            String broker_host = (System.getenv("BROKER_HOST") != null) ? System.getenv("BROKER_HOST") : "localhost";
            String broker = "tcp://" + broker_host + ":1883";
			AlgoMqttClient adapter = new AlgoMqttClient(broker, "elevator_algorithm", 0, 10000);
            adapter.run();
		} catch (ElevatorError exc) {
			System.err.println("Caught ElevatorError while running elevator algorithm MQTT client: " + exc.toString());
            exc.printStackTrace();;
		} catch (Exception exc) {
			System.err.println("Caught unhandled exception while running elevator algorithm MQTT client: " + exc.toString());
            exc.printStackTrace();;
		}
	}

    /** 
     * Destructor: Clean up connections. 
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
            connOpts.setSessionExpiryInterval(null);
            connOpts.setAutomaticReconnect(true);
            connOpts.setKeepAliveInterval(60);

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
     * Initialize and run the algorithm control loop.
     */
    public void run() {
        
        System.out.println("Starting elevator algorithm");
        connectToBroker();
        client.setCallback((MqttCallback)this);
        subscribeRetainedBuildingTopics();

        // wait for the retained topics to be read since they are needed for the building to be created
        try {
            // initial values for nrOfElevators and nrOfFloors is -1
            while (nrOfElevators < 0 || nrOfFloors < 0) {
                Thread.sleep(50);
            }
        } catch(InterruptedException exc) {
            throw new InternalError("Algorithm thread was interrupted while waiting for retained topics");
        }

        // now that we know the number of elevators we can subscribe to the elevator topics
        subscribeRetainedElevatorTopics();

        // wait for the retained topics to be read since they are needed for the building to be created
        try {
            while (maxNrPassengers.size() < nrOfElevators) {
                Thread.sleep(50);
            }
        } catch(InterruptedException exc) {
            throw new InternalError("Algorithm thread was interrupted while waiting for retained topics");
        }

        // create building and subscribe to all status topics
        building = new Building(nrOfElevators, nrOfFloors, this);
        // we wont miss anything here, because the elevator adapter publishes ALL topics repeatedly
        subscribeTopics();

        System.out.println("Setup done.");

        // run update loop
        MqttUpdateTimerTask updateTimerTask = new MqttUpdateTimerTask(() -> { if (RmiConnected) { building.updateElevatorTargets(); } return null; });
        timer.schedule(updateTimerTask, 0, targetUpdateInterval_ms);
    }

    /**
     * Stop the algorithm control loop.
     */
    public void stop() {
        timer.cancel();
    }

    /**
     * Publishes the new target floor for the given elevator.
     * @param elevatorId
     * @param target
     */
    public void publishTargetFloor(int elevatorId, int target) {
        publishControlTopic(
            elevatorId, 
            target, 
            MqttTopics.controlElevatorTopic + String.valueOf(elevatorId) + "/targetFloor"
            );
    }

    /**
     * Publishes the new committed direction for the given elevator.
     * @param elevatorId
     * @param committedDirection
     */
    public void publishCommittedDirection(int elevatorId, int committedDirection) {
        publishControlTopic(
            elevatorId, 
            committedDirection, 
            MqttTopics.controlElevatorTopic + String.valueOf(elevatorId) + "/committedDirection"
            );
    }

    /**
     * Publishes a not retained elevator control topic with Integer payload.
     * @param elevatorId ID of the elevator to publish for
     * @param payload Payload to publish
     * @param topic Topic to publish to
     */
    private void publishControlTopic(int elevatorId, int payload, String topic) {
        if (!client.isConnected()) {
            throw new MqttError("MQTT client must be connected before publishing messages");
        }
        try {
            MqttMessage msg = new MqttMessage(String.valueOf(payload).getBytes(), qos, false, null);
            IMqttToken token = client.publish(topic, msg);
            token.waitForCompletion(timeoutMs);

        } catch (MqttException exc) {
            throw new MqttError("While publishing topic a " + formatMqttException(exc));
        }
    }

    /**
     * Provides the maximum number of passengers for the given elevator.
     * @param elevatorId
     * @return
     */
    public int getMaxPassengers(int elevatorId) {
        if (elevatorId >= maxNrPassengers.size()) {
            throw new InvalidArgError("Elevator ID out of range for max number of passengers");
        }
        return maxNrPassengers.get(elevatorId);
    }

    /**
     * Subscribes to retained topics concerning the building (numberOfElevators, nrOfFloors).
     */
    private void subscribeRetainedBuildingTopics() {
        try {
            client.subscribe(MqttTopics.infoTopic + "numberOfElevators", qos);
            client.subscribe(MqttTopics.infoTopic + "numberOfFloors", qos);
            client.subscribe(MqttTopics.infoTopic + "rmiConnected", qos);
        } catch (MqttException exc) {
            throw new MqttError("While subscribing to retained topics a " + formatMqttException(exc));
        }
    }

    /**
     * Subscribes to retained topics concerning the elevators (maxPassengers).
     */
    private void subscribeRetainedElevatorTopics() {
        if (nrOfElevators < 0) {
            throw new InternalError("Number of elevators needs to be obtained before subscribing to elevator topics");
        }
        try {
            for (int id = 0; id < nrOfElevators; id++) {
                client.subscribe(MqttTopics.infoElevatorTopic + String.valueOf(id) + "/maxPassengers", qos);
            }
        } catch (MqttException exc) {
            throw new MqttError("While subscribing to retained topics a " + formatMqttException(exc));
        }
    }

    /**
     * Subscribes to all topics relevant for controlling the elevator.
     */
    private void subscribeTopics() {
        if (nrOfElevators < 0) {
            throw new InternalError("Number of elevators needs to be obtained before subscribing to elevator topics");
        }
        try {
            // elevator topics
            for (int id = 0; id < nrOfElevators; id++) {

                // committed direction
                client.subscribe(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/committedDirection", qos);

                // floor buttons in elevator
                client.subscribe(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floorButton/#", qos);
   
                // door status of elevator
                client.subscribe(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/door", qos);

                // current floor of elevator
                client.subscribe(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floor", qos);

                // load of elevator in lbs
                client.subscribe(MqttTopics.statusElevatorTopic + String.valueOf(id) + "/load/lbs", qos);
            }

            // floor topics
            client.subscribe(MqttTopics.statusFloorTopic + "#", qos);


        } catch (MqttException exc) {
            throw new MqttError("While subscribing to retained topics a " + formatMqttException(exc));
        }
    }

    /**
     * Callback function for subscribed MQTT topics.
     */
    public void messageArrived(String topic, MqttMessage msg) {
        
        // topics without ID
        if (topic.indexOf("numberOfElevators") != -1) {
            nrOfElevators = Integer.valueOf(new String(msg.getPayload()));
            return;
        }
        if (topic.indexOf("numberOfFloors") != -1) {
            nrOfFloors = Integer.valueOf(new String(msg.getPayload()));
            return;
        }
        if (topic.indexOf("rmiConnected") != -1) {
            RmiConnected = Boolean.valueOf(new String(msg.getPayload(), StandardCharsets.UTF_8));
            return;
        }

        // get ID in topic
        Matcher matcher = digitRegex.matcher(topic);
        if (!matcher.find()) {
            throw new MqttError("Could not find a elevator ID in topic: " + topic);
        }
        int id = Integer.valueOf(matcher.group());

        // set new committed direction
        if (topic.indexOf("committedDirection") != -1) {
            building.elevators[id].committedDirection = Integer.valueOf(new String(msg.getPayload()));
        }

        // handel elevator floor button request
        else if (topic.indexOf("floorButton") != -1) {
            // get floor number from topic
            if (!matcher.find()) {
                throw new MqttError("Could not find a floor button ID in topic: " + topic);
            }
            int floorButtonId = Integer.valueOf(matcher.group());
            
            // get floor request button state
            boolean buttonState = Boolean.valueOf(new String(msg.getPayload(), StandardCharsets.UTF_8));
            
            // handle request
            if (buttonState) {
                building.scheduleTarget(id, floorButtonId);
            }
        }

        // handle floor button call
        else if (topic.indexOf("button/up") != -1) {
            // get new button state
            boolean buttonState = Boolean.valueOf(new String(msg.getPayload(), StandardCharsets.UTF_8));

            // handle call
            if (buttonState) {
                building.scheduleFloor(id, Building.FLOOR_BUTTON_UP);
            }
        }

        // handle floor button call
        else if (topic.indexOf("button/down") != -1) {
            // get new button state
            boolean buttonState = Boolean.valueOf(new String(msg.getPayload(), StandardCharsets.UTF_8));
            // handle call
            if (buttonState) {
                building.scheduleFloor(id, Building.FLOOR_BUTTON_DOWN);
            }
        }

        // set new current floor
        else if (topic.indexOf("floor") != -1) {
            building.elevators[id].currentFloor = Integer.valueOf(new String(msg.getPayload()));
        }

        // set new door status
        else if (topic.indexOf("door") != -1) {
            building.elevators[id].doorStatus = Integer.valueOf(new String(msg.getPayload()));
        }

        // set new load
        else if (topic.indexOf("load/lbs") != -1) {
            building.elevators[id].load = Integer.valueOf(new String(msg.getPayload()));
        }

        // set new load
        else if (topic.indexOf("speed/feetPerSec") != -1) {
            building.elevators[id].speed = Integer.valueOf(new String(msg.getPayload()));
        }
        
        // retained topics
        else if (topic.indexOf("maxPassengers") != -1) {
            maxNrPassengers.add(id, Integer.valueOf(new String(msg.getPayload())));
        }

        else {
            System.out.println("Elevator algorithm received unhandled topic: " + topic);
        }

    }

    public void connectComplete(boolean reconnect, String serverURI) {
        System.out.println("MQTT client connected to " + serverURI);
    }

    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        System.out.println("MQTT client disconnected");
    }

    public void mqttErrorOccurred(MqttException exception) {}
    
    public void deliveryComplete(IMqttToken token) {}
    
    public void authPacketArrived(int reasonCode, MqttProperties properties) {}

    /**
     * Formats a MQTT exception to a more readable format.
     * @param exc
     * @return String containing the most relevant information about the exception
     */
    private String formatMqttException(MqttException exc) {
        String msg = "MQTT error occurred:\n\treason " + exc.getReasonCode();
        msg += "\tmsg " + exc.getMessage();
        msg += "\tloc " + exc.getLocalizedMessage();
        msg += "\tcause " + exc.getCause();
        msg += "\texcep " + exc;
        exc.printStackTrace();
        return msg;
    } 
}
