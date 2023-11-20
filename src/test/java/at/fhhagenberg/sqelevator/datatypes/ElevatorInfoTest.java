package at.fhhagenberg.sqelevator.datatypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.rmi.RemoteException;

import org.junit.jupiter.api.Test;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.ControlError;

/**
 * @see ElevatorInfo
 */
public class ElevatorInfoTest {

    private ElevatorInfo elevatorInfo;
    private IElevator elevatorControl;

    /**
     * @throws Exception
     * @see ElevatorInfo#populate(IElevator)
     */
    @Test
    void testPopulate() throws Exception {
        elevatorControl = mock(IElevator.class);
        elevatorInfo = new ElevatorInfo(1, 5); 
        
        when(elevatorControl.getElevatorCapacity(1)).thenReturn(10); 
        when(elevatorControl.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        when(elevatorControl.getElevatorAccel(1)).thenReturn(2);
        when(elevatorControl.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
        when(elevatorControl.getElevatorFloor(1)).thenReturn(3);
        when(elevatorControl.getElevatorPosition(1)).thenReturn(300);
        when(elevatorControl.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorControl.getElevatorWeight(1)).thenReturn(500);
        when(elevatorControl.getTarget(1)).thenReturn(4);
        when(elevatorControl.getElevatorButton(1, 0)).thenReturn(true);
        when(elevatorControl.getServicesFloors(1, 0)).thenReturn(true);

        
        elevatorInfo.populate(elevatorControl);

        
        assertEquals(10, elevatorInfo.maxPassengers);
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevatorInfo.committedDirection);
        assertEquals(2, elevatorInfo.acceleration);
        assertEquals(IElevator.ELEVATOR_DOORS_OPEN, elevatorInfo.doorStatus);
        assertEquals(3, elevatorInfo.floor);
        assertEquals(300, elevatorInfo.height);
        assertEquals(5, elevatorInfo.speed);
        assertEquals(500, elevatorInfo.load);
        assertEquals(4, elevatorInfo.targetFloor);
        assertTrue(elevatorInfo.floorButtons[0]);
        assertTrue(elevatorInfo.floorsService[0]);
    }

    /**
     * @throws Exception
     * @see ElevatorInfo#populate(IElevator)
     */
    @Test
    void testPopulateRemoteException() throws Exception {
        elevatorControl = mock(IElevator.class);
        elevatorInfo = new ElevatorInfo(1, 5); 
        
        when(elevatorControl.getElevatorCapacity(1)).thenThrow(new RemoteException("Test Remote Exception"));

        
        ControlError controlError = assertThrows(ControlError.class, () -> {
            elevatorInfo.populate(elevatorControl);
        });

        assertEquals("Unable to read status from elevator 1: Test Remote Exception", controlError.getMessage());
    }
}
