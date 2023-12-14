package at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.IElevator;
import at.fhhagenberg.sqelevator.Algorithm.Building;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class RequestButtonCb implements MqttActionListener {
    private Pattern digitRegex = Pattern.compile("\\d");

    public void onSuccess(IMqttToken asyncActionToken) {
        try {
            // get elevator ID from topic
            String topic = asyncActionToken.getTopics()[0];
            Matcher matcher = digitRegex.matcher(topic);
            if (!matcher.find()) {
                throw new MqttError("Could not find a elevator ID in topic: " + topic);
            }
            int elevatorId = Integer.valueOf(matcher.group());

            // get floor number from topic
            if (!matcher.find()) {
                throw new MqttError("Could not find a floor button ID in topic: " + topic);
            }
            int floorButtonId = Integer.valueOf(matcher.group());

            MqttMessage msg = asyncActionToken.getMessage();
            Building buildingInfo = (Building)asyncActionToken.getUserContext();

            // update floor service and set changed flag
            boolean buttonState = Boolean.getBoolean(new String(msg.getPayload(), StandardCharsets.UTF_8));

            // handle request
            if (buttonState) {
                if (buildingInfo.elevators[elevatorId].committedDirection == IElevator.ELEVATOR_DIRECTION_UP) {
                    if (buildingInfo.elevators[elevatorId].currentFloor < floorButtonId) {
                        // is secondary target since we are going up and target is below us
                        buildingInfo.elevators[elevatorId].addSecondaryTarget(floorButtonId);
                    } else { // target is in committed direction
                        buildingInfo.elevators[elevatorId].addPrimaryTarget(floorButtonId);
                    }
                }
                else if (buildingInfo.elevators[elevatorId].committedDirection == IElevator.ELEVATOR_DIRECTION_DOWN) {
                    if (buildingInfo.elevators[elevatorId].currentFloor > floorButtonId) {
                        // is secondary target since we are going down and target is above us
                        buildingInfo.elevators[elevatorId].addSecondaryTarget(floorButtonId);
                    } else { // target is in committed direction
                        buildingInfo.elevators[elevatorId].addPrimaryTarget(floorButtonId);
                    }
                }
                else { // committed direction is uncommitted -> not working currently -> just add target as primary
                    buildingInfo.elevators[elevatorId].addPrimaryTarget(floorButtonId);
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
