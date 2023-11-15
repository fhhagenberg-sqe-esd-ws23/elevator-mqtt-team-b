package at.fhhagenberg.sqelevator.datatypes;

import at.fhhagenberg.sqelevator.IElevator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class BuildingInfoTest {

    @Mock
    private IElevator mockElevatorControl;

    @Before
    public void setUp() throws Exception {
        mockElevatorControl = mock(IElevator.class);
    }

    @Test
    public void testPopulate() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();
        
        // Mock the behavior of IElevator methods
        when(mockElevatorControl.getFloorNum()).thenReturn(5);
        when(mockElevatorControl.getFloorHeight()).thenReturn(3);
        when(mockElevatorControl.getElevatorNum()).thenReturn(3);
        when(mockElevatorControl.getClockTick()).thenReturn(1000L);

        // Call the populate method
        buildingInfo.populate(mockElevatorControl);

        // Verify that the elevators and floors are populated
        assertEquals(3, buildingInfo.getNumberOfElevators());
        assertEquals(5, buildingInfo.getNumberOfFloors());
        assertEquals(3, buildingInfo.getFloorHeight());
        assertEquals(1000L, buildingInfo.getClockTick());
    }

    @Test
    public void testGetElevator() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getElevatorNum()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        ElevatorInfo elevatorInfo = buildingInfo.getElevator(1);
        assertNotNull(elevatorInfo);
        assertEquals(1, elevatorInfo.elevatorId);
    }

    @Test
    public void testGetFloor() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        FloorInfo floorInfo = buildingInfo.getFloor(1);
        assertNotNull(floorInfo);
        assertEquals(1, floorInfo.floorId);
    }

    
    @Test
    public void testGetNumberOfElevators() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getElevatorNum()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(3, buildingInfo.getNumberOfElevators());
    }

    @Test
    public void testGetNumberOfFloors() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorNum()).thenReturn(5);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(5, buildingInfo.getNumberOfFloors());
    }

    @Test
    public void testGetFloorHeight() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        when(mockElevatorControl.getFloorHeight()).thenReturn(3);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(3, buildingInfo.getFloorHeight());
    }

    @Test
    public void testGetClockTick() throws Exception {
        BuildingInfo buildingInfo = new BuildingInfo();

        buildingInfo.populate(mockElevatorControl);

        assertEquals(0L, buildingInfo.getClockTick());

        when(mockElevatorControl.getClockTick()).thenReturn(1000L);

        buildingInfo.populate(mockElevatorControl);

        assertEquals(1000L, buildingInfo.getClockTick());
    }
}