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
import org.mockito.junit.jupiter.MockitoExtension;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Adapter.MqttCallbacks.CallbackContext;
import at.fhhagenberg.sqelevator.Adapter.MqttCallbacks.TargetFloorCb;
import at.fhhagenberg.sqelevator.Adapter.datatypes.BuildingInfo;
import at.fhhagenberg.sqelevator.Adapter.datatypes.ElevatorInfo;
import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;

@ExtendWith(MockitoExtension.class)
public class TargetFloorCbTest {

    @Mock IMqttToken mockMqttToken;
    @Mock CallbackContext mockCallbackContext;
    @Mock ElevatorInfo mockElevatorInfo;
    @Mock MqttMessage mockMqttMessage;
    @InjectMocks TargetFloorCb targetFloorCb;

    /**
     * @throws Exception
     * @see TargetFloorCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);

        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);

        when(mockMqttMessage.getPayload()).thenReturn(ByteBuffer.allocate(4).putInt(3).array());

        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/target"});

        targetFloorCb.onSuccess(mockMqttToken);

        verify(mockCallbackContext.buildingInfo).getElevator(anyInt());
        verify(mockCallbackContext.elevatorIface).setTarget(anyInt(), anyInt());

        ArgumentCaptor<Integer> elevatorIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> targetFloorCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockCallbackContext.elevatorIface).setTarget(elevatorIdCaptor.capture(), targetFloorCaptor.capture());

        assertEquals(Integer.valueOf(1), elevatorIdCaptor.getValue());
        assertEquals(Integer.valueOf(3), targetFloorCaptor.getValue());
    }
    /**
     * @throws Exception
     * @see TargetFloorCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessElevatorIdNotFoundTest() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/target"});

        MqttError err = assertThrows(MqttError.class, () -> targetFloorCb.onSuccess(mockMqttToken));

        assertEquals("Could not find a elevator ID in topic: elevator/target", err.getMessage());
    }

    /**
     * @throws Exception
     * @see TargetFloorCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessMqttExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        MqttException mockMqttException = Mockito.mock(MqttException.class);
        when(mockMqttException.toString()).thenReturn("Mocked MQTT Exception");
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/target"});
        doThrow(mockMqttException).when(mockMqttToken).getMessage();

        MqttError err = assertThrows(MqttError.class, () -> targetFloorCb.onSuccess(mockMqttToken));

        assertEquals("MQTT exception occurred in subscription callback: Mocked MQTT Exception", err.getMessage());
    }

    /**
     * @throws Exception
     * @see TargetFloorCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessRemoteExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockMqttMessage.getPayload()).thenReturn(ByteBuffer.allocate(4).putInt(3).array());
        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/target"});

        RemoteException mockRemoteException = mock(RemoteException.class);
        when(mockRemoteException.getMessage()).thenReturn("Mocked Remote Exception");

        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);
        
        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);
        
        doThrow(mockRemoteException).when(mockCallbackContext.elevatorIface).setTarget(anyInt(), anyInt());

        ControlError err = assertThrows(ControlError.class, () -> targetFloorCb.onSuccess(mockMqttToken));

        assertEquals("Unable to set new target floor 3 of elevator 1: Mocked Remote Exception", err.getMessage());
    }

    /**
     * @throws Exception
     * @see TargetFloorCb#onFailure(IMqttToken, Throwable)
     */
    @Test
    public void onFailure() throws Exception {
        MockitoAnnotations.initMocks(this);

        Throwable mockThrowable = mock(Throwable.class);
        targetFloorCb.onFailure(mockMqttToken, mockThrowable);

        verifyNoMoreInteractions(mockThrowable);
    }
}