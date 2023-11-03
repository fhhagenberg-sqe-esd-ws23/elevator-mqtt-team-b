package at.fhhagenberg.sqelevator.datatypes;

import at.fhhagenberg.sqelevator.exceptions.*;

public class BuildingInfo {
    private ElevatorInfo[] elevators;
    private FloorInfo[] floors;
    private final int floorHeight;
    private long clockTickMs;

    public BuildingInfo(ElevatorInfo[] elevators, int numFloors, int floorHeight, long clockTickMs) {
        this.elevators = elevators;
        this.floors = new FloorInfo[numFloors];
        this.floorHeight = floorHeight;
        this.clockTickMs = clockTickMs;
    }

    public ElevatorInfo getElevator(int id) {
        if (id >= elevators.length) {
            throw new InvalidArgError("Elevator with ID " + String.valueOf(id) + " does not exist.");
        }
        return elevators[id];
    }

    public void updateElevator(int id, ElevatorInfo elevator) {
        if (id >= elevators.length) {
            throw new InvalidArgError("Elevator with ID " + String.valueOf(id) + " does not exist.");
        }
        elevators[id] = elevator;
    }

    public FloorInfo getFloor(int number) {
        if (number >= floors.length) {
            throw new InvalidArgError("Floor number " + String.valueOf(number) + " does not exist.");
        }
        return floors[number];
    }

    public int getNumberOfElevators() {
        return elevators.length;
    }

    public int getNumberOfFloors() {
        return floors.length;
    }

    public int getFloorHeight() {
        return floorHeight;
    }

    public long getClockTick() {
        return clockTickMs;
    }
}
