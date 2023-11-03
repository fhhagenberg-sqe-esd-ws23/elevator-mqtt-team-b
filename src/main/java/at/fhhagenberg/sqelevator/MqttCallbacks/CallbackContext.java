package at.fhhagenberg.sqelevator.MqttCallbacks;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.datatypes.BuildingInfo;

public class CallbackContext {
    public BuildingInfo buildingInfo;
    public IElevator elevatorIface;
}
