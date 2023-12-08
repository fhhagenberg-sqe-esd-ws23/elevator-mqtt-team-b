package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.Test;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import sqelevator.IElevator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.containers.HiveMQContainer;

//@Testcontainers
public class ElevatorMqttAdapterTest {

    @Mock private IElevator elevatorIface;
    @Mock private MqttAsyncClient mqttAsyncClient;
    //private ElevatorMqttAdapter elevatorMqttAdapter;
    //private Mqtt3BlockingClient testClient;
    //@Container
    //HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest")); // 1

    /**
     * Test method for {@link ElevatorMqttAdapter#connectToBroker()}.
     * @throws MqttException
     */
    @Test 
    void testConnectToBroker() throws MqttException {
        //testClient = Mqtt3Client.builder()
        //        .serverPort(container.getMqttPort()).buildBlocking();
        //testClient.connect();
        //MockitoAnnotations.initMocks(this);

        //elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, "Client", "broker", "Subclient", 0, 0);
    
        //IMqttToken tokenMock = mock(IMqttToken.class);

        //when(mqttAsyncClient.connect(any())).thenReturn(tokenMock);

        //elevatorMqttAdapter.connectToBroker();

        //verify(mqttAsyncClient).connect(any());
        //verify(tokenMock).waitForCompletion(anyLong());
    }

    /**
     * Test method to check if a second connection is not possible.
     * @throws MqttException
     */
    @Test
    void connectToBroker_AlreadyConnected() throws MqttException {
       // MockitoAnnotations.initMocks(this);
        
        //when(mqttAsyncClient.isConnected()).thenReturn(true);

        //elevatorMqttAdapter.connectToBroker();

        //verifyZeroInteractions(mqttAsyncClient);
    }
}
