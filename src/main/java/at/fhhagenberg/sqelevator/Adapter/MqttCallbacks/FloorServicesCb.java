package at.fhhagenberg.sqelevator.Adapter.MqttCallbacks;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import at.fhhagenberg.sqelevator.exceptions.ControlError;
import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

public class FloorServicesCb implements MqttActionListener {
    Pattern digitRegex = Pattern.compile("\\d");

    public void onSuccess(IMqttToken asyncActionToken) {
        int elevatorId = -1;
        int floorNum = -1;
        try {
            // get elevator ID from topic
            String topic = asyncActionToken.getTopics()[0];
            Matcher matcher = digitRegex.matcher(topic);
            if (!matcher.find()) {
                throw new MqttError("Could not find a elevator ID in topic: " + topic);
            }
            elevatorId = Integer.valueOf(matcher.group());

            // get floor number from topic
            if (!matcher.find()) {
                throw new MqttError("Could not find a floor number in topic: " + topic);
            }
            floorNum = Integer.valueOf(matcher.group());

            MqttMessage msg = asyncActionToken.getMessage();
            CallbackContext cbContext = (CallbackContext)asyncActionToken.getUserContext();

            // check range of floor number
            if (floorNum >= cbContext.buildingInfo.getElevator(elevatorId).floorsService.length){
                 throw new InvalidArgError("Floor number " + String.valueOf(floorNum) + " does not exist.");
            }            

            // update floor service and set changed flag
            boolean floorService = Boolean.getBoolean(new String(msg.getPayload(), StandardCharsets.UTF_8));
            cbContext.buildingInfo.getElevator(elevatorId).floorsService[floorNum] = floorService;

            cbContext.elevatorIface.setServicesFloors(elevatorId, floorNum, floorService);

        } catch (MqttException exc) {
            throw new MqttError("MQTT exception occurred in subscription callback: " + exc.toString());
        } catch (RemoteException exc) {
            throw new ControlError("Unable to set new floor service at floor " + String.valueOf(floorNum) + " of elevator " + String.valueOf(elevatorId) + ": " + exc.getMessage());
        }
    }

    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

    }
}
