package at.fhhagenberg.sqelevator.datatypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.rmi.RemoteException;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import at.fhhagenberg.sqelevator.Adapter.datatypes.FloorInfo;
import at.fhhagenberg.sqelevator.exceptions.ControlError;
import sqelevator.IElevator;

/**
 * @see FloorInfo
 */
public class FloorInfoTest {

    private FloorInfo floorInfo;
    @Mock private IElevator elevatorControl;

    /**
     * @throws Exception
     * @see FloorInfo#populate(IElevator)
     */
    @Test
    void testPopulate() throws Exception {
        MockitoAnnotations.initMocks(this);
        floorInfo = new FloorInfo(1);

        when(elevatorControl.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorControl.getFloorButtonUp(1)).thenReturn(false);

        floorInfo.populate(elevatorControl);

        assertTrue(floorInfo.callDown);
        assertFalse(floorInfo.callUp);
    }

    /**
     * @throws Exception
     * @see FloorInfo#populate(IElevator)
     */
    @Test
    void testPopulateRemoteException() throws Exception {
        MockitoAnnotations.initMocks(this);
        floorInfo = new FloorInfo(1);

        when(elevatorControl.getFloorButtonDown(1)).thenThrow(new RemoteException("Test Remote Exception"));

        ControlError controlError = assertThrows(ControlError.class, () -> {
            floorInfo.populate(elevatorControl);
        });

        assertEquals("Unable to read status from floor 1: Test Remote Exception", controlError.getMessage());
    }
}