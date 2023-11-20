package at.fhhagenberg.sqelevator.datatypes;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import java.rmi.RemoteException;
import static org.junit.Assert.*;


/**
 * @see BuildingInfo
 */
public class BuildingInfoTest {

    @Mock
    private IElevator mockElevatorControl;

    /**
     * @throws Exception
     * @see BuildingInfo#populate(IElevator)
     */
    @Test
    public void testPopulate() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        
        when(mockElevatorControl.getFloorNum()).thenReturn(5);
        when(mockElevatorControl.getFloorHeight()).thenReturn(3);
        when(mockElevatorControl.getElevatorNum()).thenReturn(3);
        when(mockElevatorControl.getClockTick()).thenReturn(1000L);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(3, buildingInfo.getNumberOfElevators());
        assertEquals(5, buildingInfo.getNumberOfFloors());
        assertEquals(3, buildingInfo.getFloorHeight());
        assertEquals(1000L, buildingInfo.getClockTick());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#populate(IElevator)
     */
    @Test
    public void testGetElevator() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getElevatorNum()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        ElevatorInfo elevatorInfo = buildingInfo.getElevator(1);
        assertNotNull(elevatorInfo);
        assertEquals(1, elevatorInfo.elevatorId);
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getElevator(int)
     */
    @Test
    public void testGetElevatorThrow() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        buildingInfo.populate(mockElevatorControl);
        
        InvalidArgError InvArgErr = assertThrows(InvalidArgError.class, () -> {
            buildingInfo.getElevator(1);
        });

        assertEquals("Elevator with ID 1 does not exist.", InvArgErr.getMessage());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getElevator(int)
     */
    @Test
    public void testGetElevatorExceedLengthThrow() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        when(mockElevatorControl.getElevatorNum()).thenReturn(3);
        buildingInfo.populate(mockElevatorControl); 
        
        ElevatorInfo elevatorInfo = buildingInfo.getElevator(1);
        assertNotNull(elevatorInfo);
        assertEquals(1, elevatorInfo.elevatorId);
        
        
        InvalidArgError InvArgErr = assertThrows(InvalidArgError.class, () -> {
            buildingInfo.getElevator(4);
        });

        assertEquals("Elevator with ID 4 does not exist.", InvArgErr.getMessage());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getFloor(int)
     */
    @Test
    public void testGetFloor() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        FloorInfo floorInfo = buildingInfo.getFloor(1);
        assertNotNull(floorInfo);
        assertEquals(1, floorInfo.floorId);
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getFloor(int)
     */
    @Test
    public void testGetFloorThrow() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        buildingInfo.populate(mockElevatorControl);
        
        InvalidArgError InvArgErr = assertThrows(InvalidArgError.class, () -> {
            buildingInfo.getFloor(1);
        });

        assertEquals("Floor number 1 does not exist.", InvArgErr.getMessage());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getFloor(int)
     */
    @Test
    public void testGetFloorExceedLengthThrow() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        
        when(mockElevatorControl.getFloorNum()).thenReturn(5);
        buildingInfo.populate(mockElevatorControl);
        
        InvalidArgError InvArgErr = assertThrows(InvalidArgError.class, () -> {
            buildingInfo.getFloor(6);
        });

        assertEquals("Floor number 6 does not exist.", InvArgErr.getMessage());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getNumberOfElevators()
     */
    @Test
    public void testGetNumberOfElevators() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getElevatorNum()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(3, buildingInfo.getNumberOfElevators());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getNumberOfFloors()
     */
    @Test
    public void testGetNumberOfFloors() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(5, buildingInfo.getNumberOfFloors());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getFloorHeight()
     */
    @Test
    public void testGetFloorHeight() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorHeight()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(3, buildingInfo.getFloorHeight());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#getClockTick()
     */
    @Test
    public void testGetClockTick() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        buildingInfo.populate(mockElevatorControl);

        assertEquals(0L, buildingInfo.getClockTick());

        when(mockElevatorControl.getClockTick()).thenReturn(1000L);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(1000L, buildingInfo.getClockTick());
    }

    /**
     * @throws Exception
     * @see BuildingInfo#populate(IElevator)
     */
    @Test
    public void testPopulateElevators() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        buildingInfo.populate(mockElevatorControl);

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        for (int i = 0; i < buildingInfo.getNumberOfElevators(); i++) {
            verify(buildingInfo.getElevator(i), times(1)).populate(mockElevatorControl);
        }
    }

    /**
     * @throws Exception
     * @see BuildingInfo#populate(IElevator)
     */
    @Test
    public void testPopulateFloors() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();
        buildingInfo.populate(mockElevatorControl);

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        for (int i = 0; i < buildingInfo.getNumberOfElevators(); i++) {
            verify(buildingInfo.getFloor(i), times(1)).populate(mockElevatorControl);
        }
    }

    /**
     * @throws Exception
     * @see BuildingInfo#populate(IElevator)
     */
    @Test
    public void testPopulateWithRemoteException() throws Exception {
        mockElevatorControl = mock(IElevator.class);
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorNum()).thenThrow(new RemoteException("Mocked Remote Exception"));

        ControlError err = assertThrows(ControlError.class, () -> buildingInfo.populate(mockElevatorControl));

        assertEquals("Unable to read status from building: Mocked Remote Exception", err.getMessage());
    }
}