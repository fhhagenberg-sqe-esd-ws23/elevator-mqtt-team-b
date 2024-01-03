package at.fhhagenberg.sqelevator;

public class MqttTopics {
    private static final String topicBase = "building/";
    public static final String infoTopic = topicBase + "info/";
    private static final String statusTopic = topicBase + "status/";
    private static final String controlTopic = topicBase + "control/";
    public static final String infoElevatorTopic = infoTopic + "elevator/";
    public static final String statusElevatorTopic = statusTopic + "elevator/";
    public static final String controlElevatorTopic = controlTopic + "elevator/";
    public static final String statusFloorTopic = statusTopic + "floor/";
}
