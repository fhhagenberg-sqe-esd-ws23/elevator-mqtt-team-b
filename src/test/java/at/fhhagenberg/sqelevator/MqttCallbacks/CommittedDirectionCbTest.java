package at.fhhagenberg.sqelevator.MqttCallbacks;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Adapter.MqttCallbacks.CallbackContext;
import at.fhhagenberg.sqelevator.Adapter.MqttCallbacks.CommittedDirectionCb;
import at.fhhagenberg.sqelevator.Adapter.datatypes.BuildingInfo;
import at.fhhagenberg.sqelevator.Adapter.datatypes.ElevatorInfo;
import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @see CommittedDirectionCb
 */
@ExtendWith(MockitoExtension.class)
class CommittedDirectionCbTest {

    @Mock IMqttToken mockMqttToken;
    @Mock MqttMessage mockMqttMessage;
    @Mock CallbackContext mockCallbackContext;    
    @Mock IElevator mockElevatorInterface;
    @Mock ElevatorInfo mockElevatorInfo;
    @InjectMocks CommittedDirectionCb committedDirectionCb;

    /**
     * @throws Exception
     * @see CommittedDirectionCb#onSuccess(IMqttToken)
     */
    @Test 
    void onSuccess_shouldUpdateCommittedDirection() throws RemoteException, MqttException {
        MockitoAnnotations.initMocks(this);

        mockCallbackContext.elevatorIface = mock(IElevator.class);
        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);

        when(mockCallbackContext.buildingInfo.getElevator(1)).thenReturn(mockElevatorInfo);

        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1"});
        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);        
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttMessage.getPayload()).thenReturn(ByteBuffer.allocate(4).putInt(2).array());
        
        committedDirectionCb.onSuccess(mockMqttToken);
        verify(mockCallbackContext.buildingInfo).getElevator(anyInt());
        verify(mockCallbackContext.elevatorIface).setCommittedDirection(anyInt(), anyInt());

        ArgumentCaptor<Integer> elevatorIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> directionCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallbackContext.elevatorIface).setCommittedDirection(elevatorIdCaptor.capture(), directionCaptor.capture());

        assertEquals(Integer.valueOf(1), elevatorIdCaptor.getValue());
        assertEquals(Integer.valueOf(2), directionCaptor.getValue());
    }

    /**
     * @throws Exception
     * @see CommittedDirectionCb#onSuccess(IMqttToken)
     */
    @Test 
    void onSuccess_shouldThrowMqttError_whenElevatorIdNotFound() {
        String topic = "invalid_topic";
        when(mockMqttToken.getTopics()).thenReturn(new String[]{topic});

        assertThrows(MqttError.class, () -> committedDirectionCb.onSuccess(mockMqttToken));
    }

    /**
     * @throws Exception
     * @see CommittedDirectionCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessMqttExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        MqttException mockMqttException = Mockito.mock(MqttException.class);
        when(mockMqttException.toString()).thenReturn("Mocked MQTT Exception");
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/target"});
        doThrow(mockMqttException).when(mockMqttToken).getMessage();

        MqttError err = assertThrows(MqttError.class, () -> committedDirectionCb.onSuccess(mockMqttToken));

        assertEquals("MQTT exception occurred in subscription callback: Mocked MQTT Exception", err.getMessage());
    }

    /**
     * @throws Exception
     * @see CommittedDirectionCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessRemoteExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockMqttMessage.getPayload()).thenReturn(ByteBuffer.allocate(4).putInt(3).array());
        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/"});


        RemoteException mockRemoteException = mock(RemoteException.class);
        //when(mockRemoteException.getMessage()).thenReturn("Mocked Remote Exception");

        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);
        
        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);
        
        doThrow(mockRemoteException).when(mockCallbackContext.elevatorIface).setCommittedDirection(anyInt(), anyInt());

        ControlError err = assertThrows(ControlError.class, () -> committedDirectionCb.onSuccess(mockMqttToken));

        assertEquals("Unable to set new committed direction '3' of elevator 1", err.getMessage());
    }

    /**
     * @throws Exception
     * @see CommittedDirectionCb#onFailure(IMqttToken, Throwable)
     */
    @Test
    public void onFailure() throws Exception {
        MockitoAnnotations.initMocks(this);

        Throwable mockThrowable = mock(Throwable.class);
        committedDirectionCb.onFailure(mockMqttToken, mockThrowable);

        verifyNoMoreInteractions(mockThrowable);
    }
}
