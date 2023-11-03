package at.fhhagenberg.sqelevator.datatypes;

import java.util.List;

public class ElevatorInfo {
    public ElevatorInfo(int maxPassengers) {
        this.maxPassengers = maxPassengers;
    }
    
    public int committedDirection;
    public int acceleration;
    public List<Boolean> floorButtons;
    public int doorStatus;
    public int floor;
    public int position;
    public int speed;
    public int load;
    public List<Boolean> floorsService;
    public int targetFloor;
    public final int maxPassengers;
}
