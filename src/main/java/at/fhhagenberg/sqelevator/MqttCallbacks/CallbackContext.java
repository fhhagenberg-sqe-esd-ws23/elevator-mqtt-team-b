package at.fhhagenberg.sqelevator.MqttCallbacks;

import at.fhhagenberg.sqelevator.datatypes.BuildingInfo;
import at.fhhagenberg.sqelevator.ElevatorMqttAdapter;

import sqelevator.IElevator;

public class CallbackContext {
    public BuildingInfo buildingInfo;
    public IElevator elevatorIface;
    public ElevatorMqttAdapter adapter = null;
}
