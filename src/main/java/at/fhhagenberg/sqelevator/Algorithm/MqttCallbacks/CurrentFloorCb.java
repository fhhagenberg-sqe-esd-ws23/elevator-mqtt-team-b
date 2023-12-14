package at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.Algorithm.Building;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class CurrentFloorCb implements MqttActionListener {
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

            // get building info (callback context)
            MqttMessage msg = asyncActionToken.getMessage();
            Building buildingInfo = (Building)asyncActionToken.getUserContext();

            // set new current floor
            int currentFloor = ByteBuffer.wrap(msg.getPayload()).getInt();
            buildingInfo.elevators[elevatorId].currentFloor = currentFloor;
        } 
        catch (MqttException exc) {
            throw new MqttError("MQTT exception occurred in subscription callback: " + exc.toString());
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
    }
}