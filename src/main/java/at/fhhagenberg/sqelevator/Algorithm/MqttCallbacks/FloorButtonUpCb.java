package at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Algorithm.Building;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class FloorButtonUpCb implements MqttActionListener {
    private Pattern digitRegex = Pattern.compile("\\d");

    public void onSuccess(IMqttToken asyncActionToken) {
        try {
            // get floor ID from topic
            String topic = asyncActionToken.getTopics()[0];
            Matcher matcher = digitRegex.matcher(topic);
            if (!matcher.find()) {
                throw new MqttError("Could not find a floor ID in topic: " + topic);
            }
            int floorId = Integer.valueOf(matcher.group());

            // get building info (callback context)
            MqttMessage msg = asyncActionToken.getMessage();
            Building buildingInfo = (Building)asyncActionToken.getUserContext();

            // set new button state
            boolean buttonState = Boolean.getBoolean(new String(msg.getPayload(), StandardCharsets.UTF_8));

            // handle call
            boolean callHandled = false;
            if (buttonState) {
                for (int i=0; i<buildingInfo.elevators.length; i++) {
                    if (callHandled) {
                        break;
                    }
                    // elevator is going down
                    if (buildingInfo.elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
                        continue; // let the next elevator handle this call
                    }

                    // elevator has no work to do, let it handle the call
                    if (buildingInfo.elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_UNCOMMITTED) {
                        // check the load
                        if (buildingInfo.elevators[i].load > buildingInfo.elevators[i].maxLoad * 0.8) {
                            continue; // do not handel call if elevator is already loaded more than 80% of the max capacity
                        }
                        buildingInfo.elevators[i].addPrimaryTarget(floorId);
                        callHandled = true;
                    }

                    // elevator is going up
                    if (buildingInfo.elevators[i].committedDirection == IElevator.ELEVATOR_DIRECTION_UP) {
                        // lets see in which floor we are
                        if (buildingInfo.elevators[i].currentFloor > floorId) {
                            continue; // let the next elevator handle this call
                        }
                        // check the load
                        if (buildingInfo.elevators[i].load > buildingInfo.elevators[i].maxLoad * 0.8) {
                            continue; // do not handel call if elevator is already loaded more than 80% of the max capacity
                        }
                        // make a stop at the floor with the pending call
                        buildingInfo.elevators[i].addPrimaryTarget(floorId);
                        callHandled = true;
                    }
                }
                // if call was not handled by an yet, randomly select an elevator and add target as secondary since it does not fit to committed direction
                if (!callHandled) {
                    Random rand = new Random();
                    int elevatorId = rand.nextInt(buildingInfo.elevators.length);
                    buildingInfo.elevators[elevatorId].addSecondaryTarget(floorId);
                }
            }
        } 
        catch (MqttException exc) {
            throw new MqttError("MQTT exception occurred in subscription callback: " + exc.toString());
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
    }
}