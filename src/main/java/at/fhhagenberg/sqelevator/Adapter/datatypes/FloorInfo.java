package at.fhhagenberg.sqelevator.Adapter.datatypes;

import java.rmi.RemoteException;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.exceptions.ControlError;

public class FloorInfo {    
    /** The floor number*/
    public int floorId;     
    /** Indicates that a person wants to go down from this floor*/
    public Boolean callDown;
    /** Indicates that a person wants to go up from this floor */
    public Boolean callUp;

    public FloorInfo(int floorId){
        this.floorId = floorId;
    }

    /**
     * Builds up the data model and reads data from the `elevatorControl`
     * This method should be called periodically to keep the data updated
     * @param elevatorControl Instance to the plc
     * @throws ControlError if data could not be read from plc
     */
    public void populate(IElevator elevatorControl){
        try{
            callDown = elevatorControl.getFloorButtonDown(floorId);
            callUp = elevatorControl.getFloorButtonUp(floorId);        
        } catch (RemoteException exc) {
            throw new ControlError("Unable to read status from floor " + String.valueOf(floorId) + ": " + exc.getMessage());
        }
    }
}
