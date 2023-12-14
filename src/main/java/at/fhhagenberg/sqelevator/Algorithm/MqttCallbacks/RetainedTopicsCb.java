package at.fhhagenberg.sqelevator.Algorithm.MqttCallbacks;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.Algorithm.AlgoMqttClient;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class RetainedTopicsCb implements MqttActionListener {
    private Pattern digitRegex = Pattern.compile("\\d");

    public void onSuccess(IMqttToken asyncActionToken) {
        try {
            String topic = asyncActionToken.getTopics()[0];
            MqttMessage msg = asyncActionToken.getMessage();
            AlgoMqttClient client = (AlgoMqttClient)asyncActionToken.getUserContext();

            if (topic.indexOf("numberOfElevators") != -1) {
                client.setNrOfElevators(ByteBuffer.wrap(msg.getPayload()).getInt());
            }
            else if (topic.indexOf("numberOfFloors") != -1) {
                client.setNrOfFloors(ByteBuffer.wrap(msg.getPayload()).getInt());
            }
            else if (topic.indexOf("maxPassengers") != -1) {
                // find elevator ID
                Matcher matcher = digitRegex.matcher(topic);
                if (!matcher.find()) {
                    throw new MqttError("Could not find a elevator ID in topic: " + topic);
                }
                int elevatorId = Integer.valueOf(matcher.group());

                client.setMaxPassengers(elevatorId, ByteBuffer.wrap(msg.getPayload()).getInt());
            }
        }
        catch (MqttException exc) {
            throw new MqttError("MQTT exception occurred in subscription callback: " + exc.toString());
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

    }
    
}
