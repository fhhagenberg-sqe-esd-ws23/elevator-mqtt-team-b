package at.fhhagenberg.sqelevator.MqttCallbacks;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

import java.util.regex.*;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;

public class CommittedDirectionCb implements MqttActionListener {
    Pattern digitRegex = Pattern.compile("\\d");

    public void onSuccess(IMqttToken asyncActionToken) {
        int elevatorId = -1;
        int newDirection = -1;
        try {
            // get elevator ID from topic
            String topic = asyncActionToken.getTopics()[0];
            Matcher matcher = digitRegex.matcher(topic);
            if (!matcher.find()) {
                throw new MqttError("Could not find a elevator ID in topic: " + topic);
            }
            elevatorId = Integer.valueOf(matcher.group());

            MqttMessage msg = asyncActionToken.getMessage();
            CallbackContext cbContext = (CallbackContext)asyncActionToken.getUserContext();

            // update committed direction and set changed flag
            newDirection = ByteBuffer.wrap(msg.getPayload()).getInt();
            cbContext.buildingInfo.getElevator(elevatorId).committedDirection = newDirection;

            cbContext.elevatorIface.setCommittedDirection(elevatorId, newDirection);

        } catch (MqttException exc) {
            throw new MqttError("MQTT exception occurred in subscription callback: " + exc.toString());
        } catch (RemoteException exc) {
            throw new ControlError("Unable to set new committed direction '" + String.valueOf(newDirection) + "' of elevator " + String.valueOf(elevatorId));
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

    }
}
