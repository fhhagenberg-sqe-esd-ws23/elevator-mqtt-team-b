package at.fhhagenberg.sqelevator.Algorithm;

import java.util.Timer;
import java.util.Vector;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.MqttTopics;
import at.fhhagenberg.sqelevator.MqttUpdateTimerTask;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.CommittedDirectionCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.CurrentFloorCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.DoorStatusCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.FloorButtonDownCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.FloorButtonUpCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.LoadCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.RequestButtonCb;
import at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks.RetainedTopicsCb;
import at.fhhagenberg.sqelevator.exceptions.ElevatorError;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class AlgoMqttClient {
    private MqttAsyncClient client;
    private MemoryPersistence persistence = new MemoryPersistence();

    private long timeoutMs;
    private int qos;
    private Boolean connected = false;

    private Timer timer = new Timer();
    private int targetUpdateInterval_ms;

    private int nrOfElevators = -1;
    private int nrOfFloors = -1;
    private Vector<Integer> maxNrPassengers = new Vector<Integer>();

    private MqttActionListener committedDirectionCb = new CommittedDirectionCb();
    private MqttActionListener currentFloorCb = new CurrentFloorCb();
    private MqttActionListener doorStatusCb = new DoorStatusCb();
    private MqttActionListener floorButtonDownCb = new FloorButtonDownCb();
    private MqttActionListener floorButtonUpCb = new FloorButtonUpCb();
    private MqttActionListener loadCb = new LoadCb();
    private MqttActionListener requestButtonCb = new RequestButtonCb();
    private MqttActionListener retainedTopicsCb = new RetainedTopicsCb();

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
            String broker = "192.168.xxx.xxx";
			AlgoMqttClient adapter = new AlgoMqttClient(broker, "elevatorAlgorithm", 2, 1000);
            adapter.run();
		} catch (ElevatorError exc) {
			System.err.println("Caught ElevatorError while running elevator algorithm MQTT client: " + exc.toString());
		} catch (Exception exc) {
			System.err.println("Caught unhandled exception while running elevator algorithm MQTT client: " + exc.toString());
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
        
        subscribeRetainedBuildingTopics(); 
        connectToBroker();

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

        // create building and then subscribe to all status topics
        // since the building is needed as callback context
        Building building = new Building(nrOfElevators, nrOfFloors, this);
        // we wont miss anything here, because the elevator adapter publishes ALL topics repeatedly
        subscribeTopics(building);

        // run update loop
        MqttUpdateTimerTask updateTimerTask = new MqttUpdateTimerTask(() -> { building.updateElevatorTargets(); return null; });
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
     * Sets the maximum allowed number of passengers for the given elevator.
     * @param elevatorId
     * @param maxPassengers
     */
    public void setMaxPassengers(int elevatorId, int maxPassengers) {
        if (maxPassengers <= 0) {
            throw new InvalidArgError("Maximum number of passengers must be a positive integer");
        }
        maxNrPassengers.add(elevatorId, maxPassengers);
    }

    /**
     * Sets the number of elevators for the building to control.
     * @param nrOfElevators
     */
    public void setNrOfElevators(int nrOfElevators) {
        if (nrOfElevators <= 0) {
            throw new InvalidArgError("Number of elevators must be a positive integer");
        }
        this.nrOfElevators = nrOfElevators;
    }

    /**
     * Sets the number of floors for the building to control.
     * @param nrOfFloors
     */
    public void setNrOfFloors(int nrOfFloors) {
        if (nrOfFloors <= 0) {
            throw new InvalidArgError("Number of floors must be a positive integer");
        }
        this.nrOfFloors = nrOfFloors;
    }

    /**
     * Subscribes to retained topics concerning the building (numberOfElevators, nrOfFloors).
     */
    private void subscribeRetainedBuildingTopics() {
        try {
            client.subscribe(
                MqttTopics.infoTopic + "numberOfElevators", 
                qos, 
                (Object)this, 
                retainedTopicsCb
            );

            client.subscribe(
                MqttTopics.infoTopic + "numberOfFloors", 
                qos, 
                (Object)this, 
                retainedTopicsCb
            );
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
                client.subscribe(
                    MqttTopics.infoElevatorTopic + String.valueOf(id) + "/maxPassengers", 
                    qos, 
                    (Object)this, 
                    retainedTopicsCb
                );
            }
        } catch (MqttException exc) {
            throw new MqttError("While subscribing to retained topics a " + formatMqttException(exc));
        }
    }

    /**
     * Subscribes to all topics relevant for controlling the elevator.
     */
    private void subscribeTopics(Building building) {
        if (nrOfElevators < 0) {
            throw new InternalError("Number of elevators needs to be obtained before subscribing to elevator topics");
        }
        try {
            // elevator topics
            for (int id = 0; id < nrOfElevators; id++) {

                // committed direction
                client.subscribe(
                    MqttTopics.statusElevatorTopic + String.valueOf(id) + "/committedDirection", 
                    qos, 
                    (Object)building, 
                    committedDirectionCb
                );

                // floor buttons in elevator
                for (int floor = 0; floor < nrOfFloors; floor++) {
                    client.subscribe(
                        MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floorButton/" + String.valueOf(floor), 
                        qos, 
                        (Object)building, 
                        requestButtonCb
                    );
                }

                // door status of elevator
                client.subscribe(
                    MqttTopics.statusElevatorTopic + String.valueOf(id) + "/door", 
                    qos, 
                    (Object)building, 
                    doorStatusCb
                );

                // current floor of elevator
                client.subscribe(
                    MqttTopics.statusElevatorTopic + String.valueOf(id) + "/floor", 
                    qos, 
                    (Object)building, 
                    currentFloorCb
                );

                // load of elevator in lbs
                client.subscribe(
                    MqttTopics.statusElevatorTopic + String.valueOf(id) + "/load/lbs", 
                    qos, 
                    (Object)building, 
                    loadCb
                );
            }

            // floor topics
            for (int floor = 0; floor < nrOfFloors; floor++) {
                // floor button up
                client.subscribe(
                    MqttTopics.statusFloorTopic + String.valueOf(floor) + "/button/up", 
                    qos, 
                    (Object)building, 
                    floorButtonUpCb
                );

                // floor button down
                client.subscribe(
                    MqttTopics.statusFloorTopic + String.valueOf(floor) + "/button/down", 
                    qos, 
                    (Object)building, 
                    floorButtonDownCb
                );
            }

        } catch (MqttException exc) {
            throw new MqttError("While subscribing to retained topics a " + formatMqttException(exc));
        }
    }

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
        return msg;
    } 
}
