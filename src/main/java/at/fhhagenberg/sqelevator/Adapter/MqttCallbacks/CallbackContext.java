package at.fhhagenberg.sqelevator.Adapter.MqttCallbacks;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Adapter.datatypes.BuildingInfo;

public class CallbackContext {
    public BuildingInfo buildingInfo;
    public IElevator elevatorIface;
}
