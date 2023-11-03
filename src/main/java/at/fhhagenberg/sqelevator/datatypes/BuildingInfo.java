package at.fhhagenberg.sqelevator.datatypes;

import at.fhhagenberg.sqelevator.exceptions.*;

public class BuildingInfo {
    private ElevatorInfo[] elevators;
    private FloorInfo[] floors;
    private final int floorHeight;
    private long clockTick;

    public BuildingInfo(int numElevators, int numFloors, int floorHeight, long clockTick) {
        this.elevators = new ElevatorInfo[numElevators];
        this.floors = new FloorInfo[numElevators];
        this.floorHeight = floorHeight;
        this.clockTick = clockTick;
    }

    public ElevatorInfo getElevator(int id) {
        if (id >= elevators.length) {
            throw new InvalidArgError("Elevator with ID " + String.valueOf(id) + " does not exist.");
        }
        return elevators[id];
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
        return clockTick;
    }
}
