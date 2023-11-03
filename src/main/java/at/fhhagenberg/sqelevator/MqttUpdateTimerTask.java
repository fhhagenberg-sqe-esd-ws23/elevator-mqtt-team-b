package at.fhhagenberg.sqelevator;

import java.util.TimerTask;
import java.util.concurrent.Callable;


public class MqttUpdateTimerTask extends TimerTask {
    private Callable<Void> updateMethod;

    public MqttUpdateTimerTask(Callable<Void> updateMethod) {
        this.updateMethod = updateMethod;
    }

    @Override
    public void run() {
        try {
        this.updateMethod.call();
        } catch (Exception exc) {
            System.err.println("Caught exception during update: " + exc.toString());
        }
    }
    
}
