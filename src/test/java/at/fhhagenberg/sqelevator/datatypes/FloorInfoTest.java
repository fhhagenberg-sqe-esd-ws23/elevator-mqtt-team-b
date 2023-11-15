package at.fhhagenberg.sqelevator.datatypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.rmi.RemoteException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.ControlError;

public class FloorInfoTest {

    private FloorInfo floorInfo;
    private IElevator elevatorControl;

    @BeforeEach
    void setUp() {
        elevatorControl = mock(IElevator.class);
        floorInfo = new FloorInfo(1);
    }

    @Test
    void testPopulate() throws Exception {
        when(elevatorControl.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorControl.getFloorButtonUp(1)).thenReturn(false);

        floorInfo.populate(elevatorControl);

        assertTrue(floorInfo.callDown);
        assertFalse(floorInfo.callUp);
    }

    @Test
    void testPopulateRemoteException() throws Exception {
        when(elevatorControl.getFloorButtonDown(1)).thenThrow(new RemoteException("Test Remote Exception"));

        ControlError controlError = assertThrows(ControlError.class, () -> {
            floorInfo.populate(elevatorControl);
        });

        assertEquals("Unable to read status from floor 1: Test Remote Exception", controlError.getMessage());
    }
}