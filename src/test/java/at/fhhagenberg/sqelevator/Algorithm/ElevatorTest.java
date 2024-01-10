package at.fhhagenberg.sqelevator.Algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import sqelevator.IElevator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;

@Testcontainers
public class ElevatorTest {
    private Elevator elevator;
    private static AlgoMqttClient algoMqttClient;

    @Container
    public static HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    @BeforeAll
    public static void setUp() {
        container.start();
        String broker = "tcp://broker.hivemq.com:1883";
        algoMqttClient = new AlgoMqttClient(broker, "test", 2, 20000);
        algoMqttClient.connectToBroker();    
    }

    @Test
    public void testSetCommittedDirectionWithPrimaryTarget() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(5);
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
    }

    @Test
    public void testSetCommittedDirectionWithCurrentFloorAsPrimaryTarget() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(1);
        elevator.currentFloor = 1;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testSetCommittedDirectionWithCurrentFloorAsPrimaryTarget2() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(1);
        elevator.addPrimaryTarget(2);
        elevator.currentFloor = 1;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
    }

    @Test
    public void testSetCommittedDirectionWithPrimaryTarget2() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(5);
        elevator.addPrimaryTarget(3);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 3;
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
                
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 5;
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(2);
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);
    }

    @Test
    public void testSetCommittedDirectionWithPrimaryTarget3() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.currentFloor = 10;

        elevator.addPrimaryTarget(5);
        elevator.addPrimaryTarget(3);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.currentFloor = 5;
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);
                
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.currentFloor = 3;
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(5);
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
    }


    @Test
    public void testSetComittedDirectionWithSecondaryTarget() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(5);
        elevator.addSecondaryTarget(3);
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 5;
        elevator.updateTarget(); 
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
                
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

    }

    @Test
    public void testSetComittedDirectionWithSecondaryTarget2() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.addPrimaryTarget(5);
        elevator.addSecondaryTarget(3);        
        elevator.addSecondaryTarget(2);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 5;
        elevator.updateTarget(); 
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
                
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.currentFloor = 3;
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

    }

    @Test
    public void testSetComittedDirectionWithSecondaryTarget3() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.currentFloor = 10;
        elevator.addPrimaryTarget(2);
        elevator.addSecondaryTarget(5);
        elevator.addSecondaryTarget(4);        
        elevator.addSecondaryTarget(3);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.currentFloor = 2;
        elevator.updateTarget(); 
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
                
        elevator.updateTarget();
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 3;
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 4;
        elevator.updateTarget();

        // Only for testreport reasons to UNCOMMITTED !!!!! normal _UP
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.currentFloor = 5;
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

    }

    @Test
    public void testValidateTarget() {
        elevator = new Elevator(1, 10, 100, null);
        assertThrows(InvalidArgError.class, () -> {
            elevator.addPrimaryTarget(-1);
        });
        assertThrows(InvalidArgError.class, () -> {
            elevator.addSecondaryTarget(11);
        });
    }
    @Test
    public void testInvalidConstructorCall() {
        assertThrows(InvalidArgError.class, () -> {
            elevator = new Elevator(1, 10, -1, null);
        });
        assertThrows(InvalidArgError.class, () -> {
            elevator = new Elevator(1, -1, 100, null);
        });
    }

    @Test
    public void testDoorStatusOrSpeedNotRightWithPrimaryTarget() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;

        elevator.addPrimaryTarget(5);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.speed = 1;

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testDoorStatusOrSpeedNotRightWithPrimaryTarget2() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.addPrimaryTarget(5);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;   

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
    }

    @Test
    public void testElevatorsDebugString() {

        PrintStream standardOut = System.out;
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outputStreamCaptor));

        elevator = new Elevator(1, 10, 100, algoMqttClient);
        elevator.debug = true;

        elevator.addPrimaryTarget(5);
        elevator.addSecondaryTarget(3);
        elevator.addSecondaryTarget(2);

        elevator.updateTarget();

        String TestStr = """
                ID: 1 [5]\r
                Committed direction: 1 2\r
                Current Floor: 1 0\r
                Speed: 1 0\r
                Doorstatus: 1 2""";

        assertEquals(TestStr, outputStreamCaptor.toString().trim());

        System.setOut(standardOut);
    }

    @Test
    public void testDoorStatusClosed() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.addPrimaryTarget(5);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;   

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.updateTarget();

        elevator.currentFloor = 5;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testElevatorsDebugString1() {

        PrintStream standardOut = System.out;
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outputStreamCaptor));

        elevator = new Elevator(1, 10, 100, algoMqttClient);

        elevator.addPrimaryTarget(5);
        
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.debug = true;
        elevator.currentFloor = 5;

        elevator.updateTarget();

        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);


        String TestStr = """
                ID: 1 [5]\r
                Committed direction: 1 0\r
                Current Floor: 1 5\r
                Speed: 1 0\r
                Doorstatus: 1 1\r
                Remove target in dir up: 5\r
                Publish commited dir: 2""";

        assertEquals(TestStr, outputStreamCaptor.toString().trim());

        System.setOut(standardOut);


    }

    @Test
    public void testDoorStatusClosed1() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.currentFloor = 5;

        elevator.addPrimaryTarget(2);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;   

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.updateTarget();

        elevator.currentFloor = 2;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testElevatorsDebugString2() {

        PrintStream standardOut = System.out;
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outputStreamCaptor));

        elevator = new Elevator(1, 10, 100, algoMqttClient);

        elevator.currentFloor = 5;

        elevator.addPrimaryTarget(2);
        
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.debug = true;
        elevator.currentFloor = 2;

        elevator.updateTarget();

        
        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);


        String TestStr = """
                ID: 1 [2]\r
                Committed direction: 1 1\r
                Current Floor: 1 2\r
                Speed: 1 0\r
                Doorstatus: 1 1\r
                Remove target in dir down: 2\r
                Publish commited dir: 2""";

        assertEquals(TestStr, outputStreamCaptor.toString().trim());

        System.setOut(standardOut);
    }

    @Test
    public void testMissingBranchesUp() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.addPrimaryTarget(2);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);


        elevator.currentFloor = 2;
        elevator.speed = 1;  
        elevator.updateTarget(); 

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);

        elevator.speed = 0;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED; 
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevator.committedDirection);
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testMissingBranchesDown() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.currentFloor = 5;

        elevator.addPrimaryTarget(2);

        elevator.updateTarget();     

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);


        elevator.currentFloor = 2;
        elevator.speed = 1;  
        elevator.updateTarget(); 

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);

        elevator.speed = 0;
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED; 
        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.committedDirection);
        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;

        elevator.updateTarget();

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);
    }

    @Test
    public void testComittedDirectionOutOfRange() {
        elevator = new Elevator(1, 10, 100, algoMqttClient);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED, elevator.committedDirection);

        elevator.doorStatus = IElevator.ELEVATOR_DOORS_OPEN;
        elevator.committedDirection = 3;

        elevator.addPrimaryTarget(2);

        elevator.updateTarget();     

        assertEquals(3, elevator.committedDirection);
    }
}

