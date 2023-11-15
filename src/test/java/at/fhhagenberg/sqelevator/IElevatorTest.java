package at.fhhagenberg.sqelevator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


public class IElevatorTest {

    private IElevator elevator;

    @Test
    public void testGetCommittedDirection() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);

        int direction = elevator.getCommittedDirection(0);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, direction);
    }

    @Test
    public void testGetElevatorAccel() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorAccel(0)).thenReturn(10);

        int acceleration = elevator.getElevatorAccel(0);

        assertEquals(10, acceleration);
    }

    @Test
    public void testGetElevatorButton() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorButton(0, 1)).thenReturn(true);

        boolean buttonStatus = elevator.getElevatorButton(0, 1);

        assertTrue(buttonStatus);
    }

    @Test
    public void testGetElevatorDoorStatus() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);

        int doorStatus = elevator.getElevatorDoorStatus(0);

        assertEquals(IElevator.ELEVATOR_DOORS_OPEN, doorStatus);
    }

    @Test
    public void testGetElevatorFloor() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorFloor(0)).thenReturn(2);

        int floor = elevator.getElevatorFloor(0);

        assertEquals(2, floor);
    }

    @Test
    public void testGetElevatorNum() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorNum()).thenReturn(3);

        int numElevators = elevator.getElevatorNum();

        assertEquals(3, numElevators);
    }

    @Test
    public void testGetElevatorPosition() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorPosition(0)).thenReturn(15);

        int position = elevator.getElevatorPosition(0);

        assertEquals(15, position);
    }

    @Test
    public void testGetElevatorSpeed() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorSpeed(0)).thenReturn(5);

        int speed = elevator.getElevatorSpeed(0);

        assertEquals(5, speed);
    }

    @Test
    public void testGetElevatorWeight() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorWeight(0)).thenReturn(500);

        int weight = elevator.getElevatorWeight(0);

        assertEquals(500, weight);
    }

    @Test
    public void testGetElevatorCapacity() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorCapacity(0)).thenReturn(10);

        int capacity = elevator.getElevatorCapacity(0);

        assertEquals(10, capacity);
    }

    @Test
    public void testGetFloorButtonDown() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorButtonDown(1)).thenReturn(true);

        boolean buttonDown = elevator.getFloorButtonDown(1);

        assertTrue(buttonDown);
    }

    @Test
    public void testGetFloorButtonUp() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorButtonUp(2)).thenReturn(false);

        boolean buttonUp = elevator.getFloorButtonUp(2);

        Assertions.assertFalse(buttonUp);
    }

    @Test
    public void testGetFloorHeight() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorHeight()).thenReturn(12);

        int floorHeight = elevator.getFloorHeight();

        assertEquals(12, floorHeight);
    }

    @Test
    public void testGetFloorNum() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorNum()).thenReturn(5);

        int numFloors = elevator.getFloorNum();

        assertEquals(5, numFloors);
    }

    @Test
    public void testGetServicesFloors() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getServicesFloors(0, 3)).thenReturn(true);

        boolean servicesFloors = elevator.getServicesFloors(0, 3);

        assertTrue(servicesFloors);
    }

    @Test
    public void testGetTarget() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getTarget(0)).thenReturn(4);

        int target = elevator.getTarget(0);

        assertEquals(4, target);
    }

    @Test
    public void testSetCommittedDirection() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);

        elevator.setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);

        verify(elevator, times(1)).setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);
    }

    @Test
    public void testSetServicesFloors() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setServicesFloors(0, 2, true);

        elevator.setServicesFloors(0, 2, true);

        verify(elevator, times(1)).setServicesFloors(0, 2, true);
    }

    @Test
    public void testSetTarget() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setTarget(0, 3);

        elevator.setTarget(0, 3);

        verify(elevator, times(1)).setTarget(0, 3);
    }
}
