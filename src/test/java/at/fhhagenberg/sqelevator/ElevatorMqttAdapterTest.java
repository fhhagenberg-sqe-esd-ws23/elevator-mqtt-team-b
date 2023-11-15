package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.Test;
import org.junit.Before;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElevatorMqttAdapterTest {

    @Mock private IElevator elevatorIface;
    @Mock private MqttAsyncClient mqttAsyncClient;

    private ElevatorMqttAdapter elevatorMqttAdapter;


    @Test void testConnectToBroker() throws MqttException {
        /*
        String broker = "broker";
        String clientId = "Client";
        String subClientId = "Subclient";
        int qos;
        long timeoutMs;  
        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, clientId, broker, subClientId, qos, timeoutMs);
    
        MockitoAnnotations.initMocks(this);
        // Mocking MqttAsyncClient behavior
        when(mqttAsyncClient.isConnected()).thenReturn(false);

        // Calling the method to be tested
        elevatorMqttAdapter.connectToBroker();

        // Verifying that MqttAsyncClient.connect is called
        verify(mqttAsyncClient).connect(any());*/
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
}
