package at.fhhagenberg.sqelevator.Algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.hivemq.client.internal.mqtt.message.publish.MqttPublish;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;

import at.fhhagenberg.sqelevator.Algorithm.Building;
import at.fhhagenberg.sqelevator.Algorithm.Elevator;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Algorithm.AlgoMqttClient;

@Testcontainers
public class BuildingTest {
    private Building building;
    private static AlgoMqttClient algoMqttClient;
    
    private static Mqtt5BlockingClient testClient;

    @Container
    public static HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));


    @BeforeAll
    public static void setUp() {

        container.start();

        testClient = Mqtt5Client.builder()
            .identifier("testClient")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();

        testClient.connect();

        
        testClient.subscribeWith().topicFilter("building/info/elevator/0/maxPassengers").send();
        testClient.subscribeWith().topicFilter("building/info/elevator/1/maxPassengers").send();
        testClient.subscribeWith().topicFilter("building/info/elevator/2/maxPassengers").send();
        
        byte [] payload = new byte[1];
        payload[0] = 10;
        
        testClient.publishWith().topic("building/info/elevator/0/maxPassengers").payload(payload).send();        
        testClient.publishWith().topic("building/info/elevator/1/maxPassengers").payload(payload).send();
        testClient.publishWith().topic("building/info/elevator/2/maxPassengers").payload(payload).send();

        

        String broker = "tcp://broker.hivemq.com:1883";
        algoMqttClient = new AlgoMqttClient(broker, "test", 2, 2000);
        algoMqttClient.connectToBroker();

        algoMqttClient.run();
        algoMqttClient.stop();
    }

    @Test
    public void testConstructBuilding() {

        building = new Building(3, 10, algoMqttClient);
        
        assertEquals(10*180, building.elevators[0].maxLoad);        
        assertEquals(10*180, building.elevators[1].maxLoad);
        assertEquals(10*180, building.elevators[2].maxLoad);
    }

    @Test
    public void testConstructWithAssert() {

        assertThrows(InvalidArgError.class, () -> new Building(0, 10, algoMqttClient));        
        assertThrows(InvalidArgError.class, () -> new Building(1, 0, algoMqttClient));

    }

    @Test
    public void testScheduleTarget() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleTarget(0, 2);        
        building.scheduleTarget(1, 4);
        building.scheduleTarget(2, 3);

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;        
        building.elevators[1].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;
        building.elevators[2].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        building.updateElevatorTargets();       

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[1].committedDirection);
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[1].committedDirection);
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[2].committedDirection);
    }

    @Test
    public void testScheduleTarget2() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleTarget(0, 2);   

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;   

        building.updateElevatorTargets();       

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.scheduleTarget(0, 4);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 2;
        building.updateElevatorTargets();
        building.elevators[0].currentFloor = 4;
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }
    @Test
    public void testScheduleTarget3() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleTarget(0, 2);   

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;   

        building.updateElevatorTargets();       

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 2;
        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.scheduleTarget(0, 1);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);
        building.scheduleTarget(0, 0);
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 1;
        building.updateElevatorTargets();
        building.elevators[0].currentFloor = 0;
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleTarget4() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleTarget(0, 2);   

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;   

        building.updateElevatorTargets();       

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.scheduleTarget(0, 4);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 2;
        building.updateElevatorTargets();

        building.scheduleTarget(0, 1);

        building.elevators[0].currentFloor = 4;
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);
        building.elevators[0].currentFloor = 1;
        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleTarget5() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleTarget(0, 2);   

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;   

        building.updateElevatorTargets();       

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 2;
        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.scheduleTarget(0, 1);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);
        building.scheduleTarget(0, 0);
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);

        building.scheduleTarget(0, 5);

        building.elevators[0].currentFloor = 1;
        building.updateElevatorTargets();
        building.elevators[0].currentFloor = 0;
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);
        building.elevators[0].currentFloor = 5;
        building.updateElevatorTargets();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloor() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleFloor(3, Building.FLOOR_BUTTON_UP);
        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN; 
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 3;

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloor2() {
        building = new Building(3, 10, algoMqttClient);

        building.scheduleFloor(3, Building.FLOOR_BUTTON_UP);
        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN; 
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.scheduleFloor(2, Building.FLOOR_BUTTON_DOWN);

        building.elevators[0].currentFloor = 3;

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloor3() {
        building = new Building(3, 10, algoMqttClient);

        building.elevators[0].currentFloor = 3;
        building.scheduleFloor(2, Building.FLOOR_BUTTON_UP);
        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN; 
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);

        building.scheduleFloor(5, Building.FLOOR_BUTTON_UP);

        building.elevators[0].currentFloor = 2;

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloor4() {
        building = new Building(3, 10, algoMqttClient);

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;
        building.elevators[0].load = 1800;
        building.scheduleFloor(2, Building.FLOOR_BUTTON_UP);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
        
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloorLoadOnMax() {
        building = new Building(3, 10, algoMqttClient);

        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN;
        building.elevators[0].load = 1800;        
        building.elevators[1].load = 1800;
        building.elevators[2].load = 1800;

        building.scheduleFloor(2, Building.FLOOR_BUTTON_UP);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
        
        building.updateElevatorTargets();

        boolean atLeastOneChanged = false;
        for (Elevator elevator : building.elevators) {
            if (elevator.committedDirection  == IElevator.ELEVATOR_DIRECTION_UP) {
                atLeastOneChanged = true;
                break;
            }
        }
        
        assertTrue(atLeastOneChanged);
    }

    @Test
    public void testScheduleFloor5() {
        building = new Building(3, 10, algoMqttClient);

        // building.elevators[0].currentFloor = 3;
        building.scheduleFloor(5, Building.FLOOR_BUTTON_UP);
        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN; 
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.scheduleFloor(7, Building.FLOOR_BUTTON_UP);

        building.elevators[0].currentFloor = 5;

        building.scheduleFloor(4, Building.FLOOR_BUTTON_UP);

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, building.elevators[0].committedDirection);

        building.elevators[0].currentFloor = 7;
        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }

    @Test
    public void testScheduleFloor6() {
        building = new Building(3, 10, algoMqttClient);

        building.elevators[0].currentFloor = 5;
        building.scheduleFloor(2, Building.FLOOR_BUTTON_DOWN);
        building.elevators[0].doorStatus = IElevator.ELEVATOR_DOORS_OPEN; 
        building.updateElevatorTargets();
        
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, building.elevators[0].committedDirection);

        building.elevators[0].load = 1800;

        building.scheduleFloor(1, Building.FLOOR_BUTTON_DOWN);

        building.elevators[0].currentFloor = 2;

        building.updateElevatorTargets();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, building.elevators[0].committedDirection);
    }
}