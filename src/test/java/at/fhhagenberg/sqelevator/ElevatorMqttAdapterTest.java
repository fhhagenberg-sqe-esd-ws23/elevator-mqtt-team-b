package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.Test;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ElevatorMqttAdapterTest {

    @Mock private IElevator elevatorIface;
    @Mock private MqttAsyncClient mqttAsyncClient;
    private ElevatorMqttAdapter elevatorMqttAdapter;
      

    @Test void testConnectToBroker() throws MqttException {
        MockitoAnnotations.initMocks(this);

        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, "Client", "broker", "Subclient", 0, 0);
    
        IMqttToken tokenMock = mock(IMqttToken.class);

        when(mqttAsyncClient.connect(any())).thenReturn(tokenMock);

        elevatorMqttAdapter.connectToBroker();

        //verify(mqttAsyncClient).connect(any());
        //verify(tokenMock).waitForCompletion(anyLong());
    }

    @Test void testDisconnectFromBroker() throws MqttException {
        /*
        String broker = "broker";
        String clientId = "Client";
        String subClientId = "Subclient";
        int qos;
        long timeoutMs;
        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, clientId, broker, subClientId, qos, timeoutMs);
    
         MockitoAnnotations.initMocks(this);
        // Mocking MqttAsyncClient behavior
        when(mqttAsyncClient.isConnected()).thenReturn(true);

        // Calling the method to be tested
        elevatorMqttAdapter.disconnectFromBroker();

        // Verifying that MqttAsyncClient.close is called
        verify(mqttAsyncClient).close();*/
    }

    @Test
    void connectToBroker_AlreadyConnected() throws MqttException {
        MockitoAnnotations.initMocks(this);
        
        when(mqttAsyncClient.isConnected()).thenReturn(true);

        elevatorMqttAdapter.connectToBroker();

        verifyZeroInteractions(mqttAsyncClient);
    }

    @Test
    void disconnectFromBroker_Success() throws MqttException {
        MockitoAnnotations.initMocks(this);
        
        when(mqttAsyncClient.isConnected()).thenReturn(true);

        elevatorMqttAdapter.disconnectFromBroker();

        verifyZeroInteractions(mqttAsyncClient);
    }

    @Test
    void disconnectFromBroker_NotConnected() throws MqttException {
        MockitoAnnotations.initMocks(this);
        
        when(mqttAsyncClient.isConnected()).thenReturn(false);

        elevatorMqttAdapter.disconnectFromBroker();

        verifyZeroInteractions(mqttAsyncClient);
    }
}
