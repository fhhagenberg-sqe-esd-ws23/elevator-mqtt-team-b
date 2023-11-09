package at.fhhagenberg.sqelevator.datatypes;

import java.rmi.RemoteException;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.ControlError;

public class ElevatorInfo {  
    public int elevatorId;
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
    public int maxPassengers;
    private boolean static_context_built = false;

    public ElevatorInfo(int elevatorId, int numberOfFloors) {
        this.elevatorId = elevatorId;
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
    }
    /**
     * Builds up the data model and reads data from the `elevatorControl`
     * This method should be called periodically to keep the data updated
     * @param elevatorControl Instance to the plc
     * @throws ControlError if data could not be read from plc
     */
    public void populate(at.fhhagenberg.sqelevator.IElevator elevatorControl){

        try{    
            if(!static_context_built){
                maxPassengers = elevatorControl.getElevatorCapacity(elevatorId);
                static_context_built = true;
            }

            committedDirection = elevatorControl.getCommittedDirection(elevatorId);
            acceleration = elevatorControl.getElevatorAccel(elevatorId);
            doorStatus = elevatorControl.getElevatorDoorStatus(elevatorId);
            floor = elevatorControl.getElevatorFloor(elevatorId);
            height = elevatorControl.getElevatorPosition(elevatorId);
            speed = elevatorControl.getElevatorSpeed(elevatorId);
            load = elevatorControl.getElevatorWeight(elevatorId);
            targetFloor = elevatorControl.getTarget(elevatorId);
            
            // read floor buttons and floor service
            for (int num = 0; num < floorButtons.length; num++) {
                floorButtons[num] = elevatorControl.getElevatorButton(elevatorId, num);
                floorsService[num] = elevatorControl.getServicesFloors(elevatorId, num);
            }
        } catch (RemoteException exc) {
            throw new ControlError("Unable to read status from elevator " + String.valueOf(elevatorId) + ": "  + exc.getMessage());
        }
        
    }
}
