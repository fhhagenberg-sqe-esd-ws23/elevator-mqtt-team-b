package at.fhhagenberg.sqelevator;

import at.fhhagenberg.sqelevator.Adapter.ElevatorMqttAdapter;
import at.fhhagenberg.sqelevator.Algorithm.AlgoMqttClient;
import sqelevator.IElevator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.rmi.RemoteException;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

@Testcontainers
public class IntegrationTest {

    @Container
    public static HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    private static Mqtt5BlockingClient testClientStart;
    private AlgoMqttClient algoMqttClient;
    private ElevatorMqttAdapter elevatorMqttAdapter;

    @Mock
    private IElevator elevatorMock;
    
    String broker = "tcp://" + container.getHost() + ":" + container.getMqttPort();

    @BeforeAll
    public static void setUp() {
        container.start();

        testClientStart = Mqtt5Client.builder()
            .identifier("testClientStart")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();
        testClientStart.connect();
    }

    @Test
    public void testRunAlgoAndAdapter() throws RemoteException {

        MockitoAnnotations.initMocks(this);

        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorMock ,broker, "test", 1, 5000);
        algoMqttClient      = new AlgoMqttClient(broker, "test", 1, 5000);

        when(elevatorMock.getElevatorNum()).thenReturn(1);
        when(elevatorMock.getElevatorFloor(0)).thenReturn(0);
        when(elevatorMock.getElevatorAccel(0)).thenReturn(15);
        when(elevatorMock.getElevatorDoorStatus(0)).thenReturn(2);
        when(elevatorMock.getElevatorPosition(0)).thenReturn(0);
        when(elevatorMock.getElevatorSpeed(0)).thenReturn(5);
        when(elevatorMock.getElevatorWeight(0)).thenReturn(10);
        when(elevatorMock.getElevatorCapacity(0)).thenReturn(5);

        when(elevatorMock.getFloorNum()).thenReturn(2);
        when(elevatorMock.getElevatorButton(0, 0)).thenReturn(false);
        when(elevatorMock.getElevatorButton(0, 1)).thenReturn(true);
        when(elevatorMock.getFloorButtonDown(0)).thenReturn(true);
        when(elevatorMock.getFloorButtonUp(0)).thenReturn(false);
        when(elevatorMock.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorMock.getFloorButtonUp(1)).thenReturn(false);
        
        when(elevatorMock.getFloorHeight()).thenReturn(3);
        when(elevatorMock.getServicesFloors(0, 0)).thenReturn(true);
        when(elevatorMock.getServicesFloors(0, 1)).thenReturn(true);

        when(elevatorMock.getTarget(0)).thenReturn(1);
        when(elevatorMock.getClockTick()).thenReturn(1000L);
        when(elevatorMock.getCommittedDirection(0)).thenReturn(1);  

        elevatorMqttAdapter.run();
        algoMqttClient.run();

        algoMqttClient.stop();
        elevatorMqttAdapter.stop();

        assertEquals(5, algoMqttClient.getMaxPassengers(0));
    }

    @Test
    public void testRunAlgoAndAdapter2() throws RemoteException {

        MockitoAnnotations.initMocks(this);

        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorMock ,broker, "test", 1, 5000);
        algoMqttClient      = new AlgoMqttClient(broker, "test", 1, 5000);

        when(elevatorMock.getElevatorNum()).thenReturn(1);
        when(elevatorMock.getElevatorFloor(0)).thenReturn(0);
        when(elevatorMock.getElevatorAccel(0)).thenReturn(15);
        when(elevatorMock.getElevatorDoorStatus(0)).thenReturn(2);
        when(elevatorMock.getElevatorPosition(0)).thenReturn(0);
        when(elevatorMock.getElevatorSpeed(0)).thenReturn(5);
        when(elevatorMock.getElevatorWeight(0)).thenReturn(10);
        when(elevatorMock.getElevatorCapacity(0)).thenReturn(5);

        when(elevatorMock.getFloorNum()).thenReturn(2);
        when(elevatorMock.getElevatorButton(0, 0)).thenReturn(false);
        when(elevatorMock.getElevatorButton(0, 1)).thenReturn(true);
        when(elevatorMock.getFloorButtonDown(0)).thenReturn(true);
        when(elevatorMock.getFloorButtonUp(0)).thenReturn(false);
        when(elevatorMock.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorMock.getFloorButtonUp(1)).thenReturn(false);
        
        when(elevatorMock.getFloorHeight()).thenReturn(3);
        when(elevatorMock.getServicesFloors(0, 0)).thenReturn(true);
        when(elevatorMock.getServicesFloors(0, 1)).thenReturn(true);

        when(elevatorMock.getTarget(0)).thenReturn(1);
        when(elevatorMock.getClockTick()).thenReturn(1000L);
        when(elevatorMock.getCommittedDirection(0)).thenReturn(1);  

        elevatorMqttAdapter.run();
        algoMqttClient.run();

        testClientStart.publishWith().topic("building/info/elevator/0/maxPassengers").payload(String.valueOf(10).getBytes()).retain(true).send();
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        algoMqttClient.stop();
        elevatorMqttAdapter.stop();  

        assertEquals(10, algoMqttClient.getMaxPassengers(0));
    }
}
