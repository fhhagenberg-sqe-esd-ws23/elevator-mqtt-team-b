package at.fhhagenberg.sqelevator.Algorithm;

import java.util.Random;

import sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;

public class Building {
    /** State variable for floor button up. */
	public final static int FLOOR_BUTTON_UP = 1;
    /** State variable for floor button down. */
	public final static int FLOOR_BUTTON_DOWN = 2;

    public Elevator[] elevators;

    /** Random generator for random elevator selection */
    private Random rand = new Random();

    public Building(int nrOfElevators, int nrOfFloors, AlgoMqttClient client) {
        if (nrOfElevators <= 0 || nrOfFloors <= 0) {
            throw new InvalidArgError("Number of elevators and number of floors must be a positive integer");
        }
        this.elevators = new Elevator[nrOfElevators];

        for (int i = 0; i < elevators.length; i++) {             
            elevators[i] = new Elevator(i, nrOfFloors, client.getMaxPassengers(i), client);                                 
        }
    }

    /**
     * Sets the 'best' next target for each elevator in the building. Must be called in a process loop.
     */
    public void updateElevatorTargets() {
        for (int i = 0; i < elevators.length; i++) {  
            elevators[i].updateTarget();
        }
    }
    
    /**
     * Schedules a target request for an elevator.
     * 
     * @param elevatorId The ID of the elevator to schedule a target for
     * @param floorId The floor ID to schedule as target.
     */
    public void scheduleTarget(int elevatorId, int floorId) {
        // elevator is currently going up
        if (elevators[elevatorId].committedDirection == IElevator.ELEVATOR_DIRECTION_UP) {
            if (elevators[elevatorId].currentFloor > floorId) {
                // is secondary target since we are going up and target is below us
                elevators[elevatorId].addSecondaryTarget(floorId);
            } else { // target is in committed direction
                elevators[elevatorId].addPrimaryTarget(floorId);
            }
        }
        // elevator is currently going down
        else if (elevators[elevatorId].committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
            if (elevators[elevatorId].currentFloor < floorId) {
                // is secondary target since we are going down and target is above us
                elevators[elevatorId].addSecondaryTarget(floorId);
            } else { // target is in committed direction
                elevators[elevatorId].addPrimaryTarget(floorId);
            }
        }
        // elevator has currently no committed direction
        else { // committed direction is uncommitted -> not working currently -> just add target as primary
            elevators[elevatorId].addPrimaryTarget(floorId);
        }
    }

    /** Selects an elevator to handle a call from a floor up or down button.
     * 
     * @param floorId The id of the floor the call came from
     * @param buttonType The type of the button that was pressed (up or down)
     */
    public void scheduleFloor(int floorId, int buttonType) {
        boolean callHandled = false;
        for (int i=0; i<elevators.length; i++) {
            if (callHandled) {
                break;
            }
            // elevator is going in the opposite direction
            if (buttonType == FLOOR_BUTTON_DOWN && elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_UP
                || buttonType == FLOOR_BUTTON_UP && elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
                continue; // let the next elevator handle this call
            }

            // elevator has no work to do, let it handle the call
            if (elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_UNCOMMITTED) {
                // check the load
                if (elevators[i].load > elevators[i].maxLoad * 0.8) {
                    continue; // do not handel call if elevator is already loaded more than 80% of the max capacity
                }
                elevators[i].addPrimaryTarget(floorId);
                callHandled = true;
            }

            // elevator is going in the same direction as the request
            if (buttonType == FLOOR_BUTTON_DOWN && elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN
                || buttonType == FLOOR_BUTTON_UP && elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_UP) {
                // lets see if we are already passed this floor
                if (buttonType == FLOOR_BUTTON_DOWN && elevators[i].currentFloor <= floorId
                    || buttonType == FLOOR_BUTTON_UP && elevators[i].currentFloor >= floorId) {
                    continue; // let the next elevator handle this call
                }
                // check the load
                if (elevators[i].load > elevators[i].maxLoad * 0.8) {
                    continue; // do not handel call if elevator is already loaded more than 80% of the max capacity
                }
                // make a stop at the floor with the pending call
                elevators[i].addPrimaryTarget(floorId);
                callHandled = true;
            }
        }
        // if call was not handled by an yet, randomly select an elevator and add target as secondary since it does not fit to committed direction
        if (!callHandled) {
            int elevatorId = this.rand.nextInt(elevators.length);
            elevators[elevatorId].addSecondaryTarget(floorId);
        }
    }
}
