package at.fhhagenberg.sqelevator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


public class IElevatorTest {

    private IElevator elevator;

    /*
     * Test methods for IElevator interface
     */

     /**
        * @throws Exception
        * @see IElevator#getCommittedDirection(int)
      */
    @Test
    public void testGetCommittedDirection() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);

        int direction = elevator.getCommittedDirection(0);

        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, direction);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorAccel(int)
     */
    @Test
    public void testGetElevatorAccel() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorAccel(0)).thenReturn(10);

        int acceleration = elevator.getElevatorAccel(0);

        assertEquals(10, acceleration);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorButton(int, int)
     */
    @Test
    public void testGetElevatorButton() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorButton(0, 1)).thenReturn(true);

        boolean buttonStatus = elevator.getElevatorButton(0, 1);

        assertTrue(buttonStatus);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorDoorStatus(int)
     */
    @Test
    public void testGetElevatorDoorStatus() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);

        int doorStatus = elevator.getElevatorDoorStatus(0);

        assertEquals(IElevator.ELEVATOR_DOORS_OPEN, doorStatus);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorFloor(int)
     */
    @Test
    public void testGetElevatorFloor() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorFloor(0)).thenReturn(2);

        int floor = elevator.getElevatorFloor(0);

        assertEquals(2, floor);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorNum()
     */
    @Test
    public void testGetElevatorNum() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorNum()).thenReturn(3);

        int numElevators = elevator.getElevatorNum();

        assertEquals(3, numElevators);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorPosition(int)
     */
    @Test
    public void testGetElevatorPosition() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorPosition(0)).thenReturn(15);

        int position = elevator.getElevatorPosition(0);

        assertEquals(15, position);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorSpeed(int)
     */
    @Test
    public void testGetElevatorSpeed() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorSpeed(0)).thenReturn(5);

        int speed = elevator.getElevatorSpeed(0);

        assertEquals(5, speed);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorWeight(int)
     */
    @Test
    public void testGetElevatorWeight() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorWeight(0)).thenReturn(500);

        int weight = elevator.getElevatorWeight(0);

        assertEquals(500, weight);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getElevatorCapacity(int)
     */
    @Test
    public void testGetElevatorCapacity() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getElevatorCapacity(0)).thenReturn(10);

        int capacity = elevator.getElevatorCapacity(0);

        assertEquals(10, capacity);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getFloorButtonDown(int)
     */
    @Test
    public void testGetFloorButtonDown() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorButtonDown(1)).thenReturn(true);

        boolean buttonDown = elevator.getFloorButtonDown(1);

        assertTrue(buttonDown);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getFloorButtonUp(int)
     */
    @Test
    public void testGetFloorButtonUp() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorButtonUp(2)).thenReturn(false);

        boolean buttonUp = elevator.getFloorButtonUp(2);

        Assertions.assertFalse(buttonUp);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getFloorHeight()
     */
    @Test
    public void testGetFloorHeight() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorHeight()).thenReturn(12);

        int floorHeight = elevator.getFloorHeight();

        assertEquals(12, floorHeight);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getFloorNum()
     */
    @Test
    public void testGetFloorNum() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getFloorNum()).thenReturn(5);

        int numFloors = elevator.getFloorNum();

        assertEquals(5, numFloors);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getServicesFloors(int, int)
     */
    @Test
    public void testGetServicesFloors() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getServicesFloors(0, 3)).thenReturn(true);

        boolean servicesFloors = elevator.getServicesFloors(0, 3);

        assertTrue(servicesFloors);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getTarget(int)
     */
    @Test
    public void testGetTarget() throws Exception {
        elevator = mock(IElevator.class);

        when(elevator.getTarget(0)).thenReturn(4);

        int target = elevator.getTarget(0);

        assertEquals(4, target);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#getTargetDirection(int)
     */
    @Test
    public void testSetCommittedDirection() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);

        elevator.setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);

        verify(elevator, times(1)).setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_DOWN);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#setServicesFloors(int, int, boolean)
     */
    @Test
    public void testSetServicesFloors() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setServicesFloors(0, 2, true);

        elevator.setServicesFloors(0, 2, true);

        verify(elevator, times(1)).setServicesFloors(0, 2, true);
    }

    /**
     * 
     * @throws Exception
     * @see IElevator#setTarget(int, int)
     */
    @Test
    public void testSetTarget() throws Exception {
        elevator = mock(IElevator.class);

        doNothing().when(elevator).setTarget(0, 3);

        elevator.setTarget(0, 3);

        verify(elevator, times(1)).setTarget(0, 3);
    }
}
