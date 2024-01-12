package at.fhhagenberg.sqelevator.Algorithm;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Queue;
import java.util.LinkedList;

import sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;

public class Elevator {
    public int committedDirection;
    public int load;
    public int maxLoad;
    public int currentFloor;
    public int doorStatus;
    public int speed;

    private int id;
    private int nrOfFloors;
    /** In this set all targets matching the current committed direction are added */
    private SortedSet<Integer> primaryTargets;
    /** In this queue all targets that do not match the current committed direction are stored for later */
    private Queue<Integer> secondaryTargets;
    /** The MQTT client */
    private AlgoMqttClient client;

    public boolean debug = false;


    public Elevator(int id, int nrOfFloors, int maxNrPassengers, AlgoMqttClient client) {
        if (maxNrPassengers <= 0 || nrOfFloors <= 0) {
            throw new InvalidArgError("Number of max passengers and number of floors must be a positive integer");
        }
        this.id = id;
        this.committedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        this.load = 0;
        this.maxLoad = maxNrPassengers * 180; // average person weighs 180 lbs
        this.currentFloor = 0;
        this.doorStatus = IElevator.ELEVATOR_DOORS_CLOSED;
        this.speed = 0;

        this.nrOfFloors = nrOfFloors;
        this.primaryTargets = new TreeSet<Integer>(); 
        this.secondaryTargets = new LinkedList<Integer>();
        this.client = client;
    }

    /**
     * Add the given target to primary targets of the elevator.
     * @param target
     */
    public void addPrimaryTarget(int target) {
        validateTarget(target);
        primaryTargets.add(target);
    }
    /**
     * Add the given target to secondary targets of the elevator.
     * @param target
     */
    public void addSecondaryTarget(int target) {
        validateTarget(target);
        secondaryTargets.add(target);
    }
    
    /**
     * Set and publish the 'best' next target for the elevator. Must be called in a process loop.
     */
    public void updateTarget() {
        // update target if current floor == target and floor queue is not empty
        if (!primaryTargets.isEmpty()) {

            if(debug){
                System.out.println("ID: " + id + " " + primaryTargets);
                System.out.println("Committed direction: " + id + " " + committedDirection);
                System.out.println("Current Floor: " + id + " " + currentFloor);
                System.out.println("Speed: " + id + " " + speed);
                System.out.println("Doorstatus: " + id + " " + doorStatus);
            }

            // if not driving add target
            if (committedDirection == IElevator.ELEVATOR_DIRECTION_UNCOMMITTED) {            
                // set new target if elevator arrived at current target and doors are open
                // current target is the lowest floor in the sorted target set if going up
                if (speed == 0 && doorStatus == IElevator.ELEVATOR_DOORS_OPEN) {
                                                        
                    if(currentFloor == primaryTargets.first()){                    
                        // target handled -> remove it
                        primaryTargets.remove(primaryTargets.first()); 

                        // return if primary targets are empty now
                        if (primaryTargets.isEmpty()) {
                            // set committed direction to uncommitted
                            setCommittedDirection(currentFloor);
                            return;
                        }
                        setCommittedDirection(primaryTargets.first());
                        client.publishTargetFloor(id, primaryTargets.first());
                    }
                    setCommittedDirection(primaryTargets.first());
                    client.publishTargetFloor(id, primaryTargets.first());
                }
            }

            
            else if (committedDirection == IElevator.ELEVATOR_DIRECTION_UP) {
                // always set nearest floor in committed direction as target
                // might be update from button callbacks
                // only update if elevator is moving --> doors are closed
                if (doorStatus == IElevator.ELEVATOR_DOORS_CLOSED)
                    client.publishTargetFloor(id, primaryTargets.first());

                // set new target if elevator arrived at current target and doors are open
                // current target is the lowest floor in the sorted target set if going up
                if (currentFloor == primaryTargets.first()
                    && speed == 0
                    && (doorStatus == IElevator.ELEVATOR_DOORS_OPEN)) {
                    
                    if(debug)
                        System.out.println("Remove target in dir up: " + primaryTargets.first());
                    // target handled -> remove it
                    primaryTargets.remove(primaryTargets.first());                     

                    // return if primary targets are empty now
                    if (primaryTargets.isEmpty()) {
                        // set committed direction to uncommitted
                        setCommittedDirection(currentFloor);
                        return;
                    }

                    setCommittedDirection(primaryTargets.first());
                    client.publishTargetFloor(id, primaryTargets.first());
                }
            }

            else if (committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
                // always set nearest floor in committed direction as target
                // might be update from button callbacks
                // only update if elevator is moving --> doors are closed
                if (doorStatus == IElevator.ELEVATOR_DOORS_CLOSED)
                    client.publishTargetFloor(id, primaryTargets.last());
                
                // set new target if elevator arrived at current target and doors are open
                // current target is the highest floor in the sorted target set if going down
                if (currentFloor == primaryTargets.last()
                    && speed == 0
                    && (doorStatus == IElevator.ELEVATOR_DOORS_OPEN)) {

                    if(debug)
                        System.out.println("Remove target in dir down: " + primaryTargets.last());                    
                    // target handled -> remove it
                    primaryTargets.remove(primaryTargets.last());                    

                    // return if primary targets are empty now
                    if (primaryTargets.isEmpty()) {
                        // set committed direction to uncommitted
                        setCommittedDirection(currentFloor);
                        return;
                    }

                    setCommittedDirection(primaryTargets.last());
                    client.publishTargetFloor(id, primaryTargets.last());
                }
            }
        }

        // in case all primary targets are handled -> lets check the secondary ones
        else if (!secondaryTargets.isEmpty()) {
            // here we are done with all primary requests so we start over with no committed direction
            // just take the first request in the queue and set it as target
            // new committed direction is a result of the target and the current floor
            int target = secondaryTargets.remove();
            setCommittedDirection(target);
            client.publishTargetFloor(id, target);

            // add all targets on the way from the secondary to the primary targets (if still some left)
            SortedSet<Integer>targetsOnTheWay = null;
            if (!secondaryTargets.isEmpty()) {
                SortedSet<Integer> secondaryTargetsSorted = new TreeSet<>(secondaryTargets);
                if (committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
                    targetsOnTheWay = secondaryTargetsSorted.subSet(0, currentFloor-1);
                }
                else {
                    targetsOnTheWay = secondaryTargetsSorted.subSet(currentFloor, nrOfFloors);
                }
                // add current target to the targets on the way
                // might get lost if nearer target exists
                targetsOnTheWay.add(target);
                
                // get subset of targets on the way and add them to primary targets
                primaryTargets = new TreeSet<Integer>(targetsOnTheWay);

                // remove all secondary targets which were added to the primary targets
                secondaryTargets.removeAll(primaryTargets);
            }
        }

        // nothing to do
        else {
            // set committed direction to uncommitted
            setCommittedDirection(currentFloor);
        }
    }

    /**
     * Set and publish the committed direction according to current floor and target floor.
     */
    private void setCommittedDirection(int target) {
        if (target > currentFloor) {
            committedDirection = IElevator.ELEVATOR_DIRECTION_UP;
        }
        else if (target < currentFloor) {
            committedDirection = IElevator.ELEVATOR_DIRECTION_DOWN;
        }
        else {
            committedDirection = IElevator.ELEVATOR_DIRECTION_UNCOMMITTED;
        }
        
        if(debug)
            System.out.println("Publish commited dir: " + committedDirection);
        client.publishCommittedDirection(id, committedDirection);
    }

    /**
     * Check if the given target is in range of valid targets for this elevator.
     * @param target
     */
    private void validateTarget(int target) {
        if (target < 0 || target >= nrOfFloors) {
            throw new InvalidArgError("Target out of range: must be between zero and number of floors - 1");
        }
    }
    
}
