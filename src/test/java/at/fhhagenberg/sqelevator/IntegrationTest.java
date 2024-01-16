package at.fhhagenberg.sqelevator;

import at.fhhagenberg.sqelevator.Adapter.ElevatorMqttAdapter;
import at.fhhagenberg.sqelevator.Algorithm.AlgoMqttClient;
import sqelevator.IElevator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
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

    private AlgoMqttClient algoMqttClient;
    private ElevatorMqttAdapter elevatorMqttAdapter;

    @Mock
    private IElevator elevatorMock;
    
    String broker = "tcp://" + container.getHost() + ":" + container.getMqttPort();

    @BeforeAll
    public static void setUp() {
        container.start();
    }

    @Test
    public void testRunAlgoAndAdapter() throws RemoteException {

        MockitoAnnotations.initMocks(this);

        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorMock ,broker, "testadapt", 1, 5000);
        algoMqttClient      = new AlgoMqttClient(broker, "testalgo", 1, 5000);

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
        when(elevatorMock.getFloorHeight()).thenReturn(3);


        elevatorMqttAdapter.run();
        algoMqttClient.run();
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(elevatorMock,atLeastOnce()).setTarget(0, 1);

        algoMqttClient.stop();
        elevatorMqttAdapter.stop();

        assertEquals(5, algoMqttClient.getMaxPassengers(0));
    }
}
