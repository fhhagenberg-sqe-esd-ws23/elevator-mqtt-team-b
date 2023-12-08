package at.fhhagenberg.sqelevator.datatypes;

import java.rmi.RemoteException;

import at.fhhagenberg.sqelevator.exceptions.*;
import sqelevator.IElevator;

public class BuildingInfo {
    private ElevatorInfo[] elevators;
    private FloorInfo[] floors;
    private int floorHeight;
    private long clockTickMs;
    private boolean static_context_built = false;

    /**
     * Builds up the data model and reads data from the `elevatorControl`
     * This method should be called periodically to keep the data updated
     * @param elevatorControl Instance to the plc
     * @throws ControlError if data could not be read from plc
     */
    public void populate(IElevator elevatorControl){

        try{
            // build up the data context only once 
            if(!static_context_built){

                int num_floors = elevatorControl.getFloorNum();
                
                floorHeight = elevatorControl.getFloorHeight();
                floors = new FloorInfo[num_floors];
                elevators = new ElevatorInfo[elevatorControl.getElevatorNum()];

                // create elevators
                for (int i = 0; i < elevators.length; i++) {                
                    elevators[i] = new ElevatorInfo(i, num_floors);                                 
                }

                // create floors
                for (int i = 0; i < floors.length; i++) {
                    floors[i] = new FloorInfo(i);
                }
                static_context_built = true;
            }        

            clockTickMs = elevatorControl.getClockTick();

            // populate elevators
            for (ElevatorInfo elevatorInfo : elevators) {
                elevatorInfo.populate(elevatorControl);
            }

            // populate floors
            for (FloorInfo floorInfo : floors) {
                floorInfo.populate(elevatorControl);
            }
        }
        catch(RemoteException exc){
            throw new ControlError("Unable to read status from building: "  + exc.getMessage());
        }

    }

    /**
     * Get the elevator with the given id
     * @param id
     * @return The found elevator
     * @throws InvalidArgError if elevator with given id does not exist
     */
    public ElevatorInfo getElevator(int id) {
        if (id >= elevators.length) {
            throw new InvalidArgError("Elevator with ID " + String.valueOf(id) + " does not exist.");
        }
        return elevators[id];
    }

    /**
     * Get the current floor info by the floor number
     * @param number
     * @return The found floor info object
     * @throws InvalidArgError if floor with given id does not exist
     */
    public FloorInfo getFloor(int number) {
        if (number >= floors.length) {
            throw new InvalidArgError("Floor number " + String.valueOf(number) + " does not exist.");
        }
        return floors[number];
    }

    /**     
     * @return Number of elevators in the building
     */
    public int getNumberOfElevators() {
        return elevators.length;
    }

    /**     
     * @return Number of floors in the building
     */
    public int getNumberOfFloors() {
        return floors.length;
    }

    /**     
     * @return Height of each floor in the building
     */
    public int getFloorHeight() {
        return floorHeight;
    }

    /**     
     * @return Current clock tick of plc [ms]
     */
    public long getClockTick() {
        return clockTickMs;
    }
}
