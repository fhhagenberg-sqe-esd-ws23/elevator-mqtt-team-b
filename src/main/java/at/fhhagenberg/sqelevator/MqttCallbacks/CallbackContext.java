package at.fhhagenberg.sqelevator.MqttCallbacks;

import at.fhhagenberg.sqelevator.datatypes.BuildingInfo;
import sqelevator.IElevator;

public class CallbackContext {
    public BuildingInfo buildingInfo;
    public IElevator elevatorIface;
}
