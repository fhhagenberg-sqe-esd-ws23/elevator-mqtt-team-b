package at.fhhagenberg.sqelevator.MqttCallbacks;

import at.fhhagenberg.sqelevator.datatypes.BuildingInfo;
import at.fhhagenberg.sqelevator.datatypes.ElevatorInfo;
import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;
import sqelevator.IElevator;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;


/**
 * @see FloorServicesCb
 */
@ExtendWith(MockitoExtension.class)
public class FloorServicesCbTest {

    @Mock private IMqttToken mockMqttToken;
    @Mock private MqttMessage mockMqttMessage;
    @Mock private CallbackContext mockCallbackContext;
    @Mock private ElevatorInfo mockElevatorInfo;
    @Mock private MqttError mqttErrorMock;
    @Mock private ControlError controlErrorMock;

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);
        
        mockElevatorInfo.floorsService = new Boolean[5];
        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);

        when(mockMqttMessage.getPayload()).thenReturn("true".getBytes());

        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/floor/0"});

        FloorServicesCb floorServicesCbSpy = Mockito.spy(new FloorServicesCb());
        floorServicesCbSpy.onSuccess(mockMqttToken);

        verify(mockCallbackContext.buildingInfo, atLeastOnce()).getElevator(anyInt());
        verify(mockCallbackContext.elevatorIface).setServicesFloors(anyInt(), anyInt(), anyBoolean());

        ArgumentCaptor<Integer> elevatorIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> floorNumCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Boolean> floorServiceCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(mockCallbackContext.elevatorIface).setServicesFloors(elevatorIdCaptor.capture(), floorNumCaptor.capture(), floorServiceCaptor.capture());

        assertEquals(Integer.valueOf(1), elevatorIdCaptor.getValue());
        assertEquals(Integer.valueOf(0), floorNumCaptor.getValue());
        assertEquals(false, floorServiceCaptor.getValue());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessElevatorIdNotFoundTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/floor"});

        FloorServicesCb floorServicesCbSpy = spy(new FloorServicesCb());

        MqttError err = assertThrows(MqttError.class, () -> floorServicesCbSpy.onSuccess(mockMqttToken));

        assertEquals("Could not find a elevator ID in topic: elevator/floor", err.getMessage());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessFloorNumberNotFoundTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/floor"});

        FloorServicesCb floorServicesCbSpy = spy(new FloorServicesCb());

        MqttError err = assertThrows(MqttError.class, () -> floorServicesCbSpy.onSuccess(mockMqttToken));

        assertEquals("Could not find a floor number in topic: elevator/1/floor", err.getMessage());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessMqttExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        MqttException mockMqttException = Mockito.mock(MqttException.class);
        when(mockMqttException.toString()).thenReturn("Mocked MQTT Exception");
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/floor/0"});
        doThrow(mockMqttException).when(mockMqttToken).getMessage();

        FloorServicesCb floorServicesCbSpy = spy(new FloorServicesCb());

        MqttError err = assertThrows(MqttError.class, () -> floorServicesCbSpy.onSuccess(mockMqttToken));

        assertEquals("MQTT exception occurred in subscription callback: Mocked MQTT Exception", err.getMessage());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessRemoteExceptionTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);
        mockCallbackContext.adapter = null;

        mockElevatorInfo.floorsService = new Boolean[5];
        when(mockMqttMessage.getPayload()).thenReturn(ByteBuffer.allocate(4).putInt(3).array());
        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/floor/0"});
        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);
        
        RemoteException mockRemoteException = mock(RemoteException.class);
        doThrow(mockRemoteException).when(mockCallbackContext.elevatorIface).setServicesFloors(anyInt(), anyInt() ,anyBoolean()); 

        FloorServicesCb floorServicesCbSpy = spy(new FloorServicesCb());
        ControlError err = assertThrows(ControlError.class, () -> floorServicesCbSpy.onSuccess(mockMqttToken));

        assertEquals("Lost RMI connection to elevator, unable to reconnect, callback context was not initialized yet", err.getMessage());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onSuccess(IMqttToken)
     */
    @Test
    public void onSuccessInvalidArgErrorTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockCallbackContext.buildingInfo = mock(BuildingInfo.class);
        mockCallbackContext.elevatorIface = mock(IElevator.class);
        when(mockMqttToken.getMessage()).thenReturn(mockMqttMessage);
        when(mockMqttToken.getUserContext()).thenReturn(mockCallbackContext);
        when(mockMqttToken.getTopics()).thenReturn(new String[]{"elevator/1/floor/10"});
        when(mockCallbackContext.buildingInfo.getElevator(anyInt())).thenReturn(mockElevatorInfo);

        mockElevatorInfo.floorsService = new Boolean[1];

        FloorServicesCb floorServicesCbSpy = Mockito.spy(new FloorServicesCb());

        InvalidArgError err = assertThrows(InvalidArgError.class, () -> floorServicesCbSpy.onSuccess(mockMqttToken));

        assertEquals("Floor number 1 does not exist.", err.getMessage());
    }

    /**
     * @throws Exception
     * @see FloorServicesCb#onFailure(IMqttToken, Throwable)
     */
    @Test
    public void onFailure() throws Exception {
        MockitoAnnotations.initMocks(this);

        FloorServicesCb floorServicesCbSpy = spy(new FloorServicesCb());

        Throwable mockThrowable = mock(Throwable.class);
        floorServicesCbSpy.onFailure(mockMqttToken, mockThrowable);

        verifyNoMoreInteractions(mockThrowable);
    }
}