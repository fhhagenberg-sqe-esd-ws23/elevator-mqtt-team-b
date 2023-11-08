package at.fhhagenberg.sqelevator.datatypes;

import at.fhhagenberg.sqelevator.IElevator;

public class ElevatorInfo {  
    public int committedDirection;
    public int acceleration;
    public Boolean[] floorButtons;
    public int doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;
    public int floor;
    public int height;
    public int speed;
    public int load;
    public Boolean[] floorsService;
    public int targetFloor;
    public final int maxPassengers;

    public ElevatorInfo(int maxPassengers, int numberOfFloors) {
        this.committedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        this.acceleration = 0;
        this.floorButtons = new Boolean[numberOfFloors];
        this.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;
        this.floor = 0;
        this.height = 0;
        this.speed = 0;
        this.load = 0;
        this.floorsService = new Boolean[numberOfFloors];
        this.targetFloor = 0;
        this.maxPassengers = maxPassengers;
    }
}
