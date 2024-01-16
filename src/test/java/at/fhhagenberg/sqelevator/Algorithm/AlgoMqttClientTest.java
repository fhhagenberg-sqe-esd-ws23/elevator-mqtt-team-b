package at.fhhagenberg.sqelevator.Algorithm;

import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeAll;

@Testcontainers
public class AlgoMqttClientTest {

    private AlgoMqttClient algoMqttClient;
    private Mqtt5BlockingClient testClient;
    String broker = "tcp://" + container.getHost() + ":" + container.getMqttPort();
    
    private static Mqtt5BlockingClient testClientStart;
    

    @Container
    public static HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    @BeforeAll
    public static void setUp() {

        container.start();

        testClientStart = Mqtt5Client.builder()
            .identifier("testClientStart")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();
        testClientStart.connect();

        testClientStart.publishWith().topic("building/info/elevator/0/maxPassengers").payload(String.valueOf(10).getBytes()).retain(true).send();        
        testClientStart.publishWith().topic("building/info/elevator/1/maxPassengers").payload(String.valueOf(10).getBytes()).retain(true).send();
        testClientStart.publishWith().topic("building/info/elevator/2/maxPassengers").payload(String.valueOf(10).getBytes()).retain(true).send();

        testClientStart.publishWith().topic("building/info/numberOfElevators").payload(String.valueOf(3).getBytes()).retain(true).send();
        testClientStart.publishWith().topic("building/info/numberOfFloors").payload(String.valueOf(10).getBytes()).retain(true).send();
        testClientStart.publishWith().topic("building/info/rmiConnected").payload(String.valueOf(true).getBytes()).retain(true).send();
    }

    @Test
    public void testPublishTargetFloor() throws InterruptedException {
        
        algoMqttClient = new AlgoMqttClient(broker, "test", 1, 5000);
        algoMqttClient.connectToBroker();

        testClient = Mqtt5Client.builder()
            .identifier("testClient")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();
        testClient.connect();

        testClient.subscribeWith().topicFilter("building/control/elevator/1/targetFloor").send();

        Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 

        
        algoMqttClient.publishTargetFloor(1, 5);

        Mqtt5Publish received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/control/elevator/1/targetFloor", received.getTopic().toString());
        assertEquals("5", payload); 
    }

    @Test
    public void testPublishCommittedDirection() throws InterruptedException {

        algoMqttClient = new AlgoMqttClient(broker, "test", 1, 5000);
        algoMqttClient.connectToBroker();

        testClient = Mqtt5Client.builder()
            .identifier("testClient")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();
        testClient.connect();

        testClient.subscribeWith().topicFilter("building/control/elevator/1/committedDirection").send();

        Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 


        int elevatorId = 1;
        int committedDirection = 1;
        algoMqttClient.publishCommittedDirection(elevatorId, committedDirection);
        
        
        Mqtt5Publish received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/control/elevator/1/committedDirection", received.getTopic().toString());
        assertEquals("1", payload); 
    }

    @Test
    public void testGetMaxPassengersAssert() {
        algoMqttClient = new AlgoMqttClient(broker, "test", 1, 5000);
        algoMqttClient.connectToBroker();

        assertThrows(InvalidArgError.class, ()->{ algoMqttClient.getMaxPassengers(0);});
    }

    @Test
    public void testMessageArrived(){     

        algoMqttClient = new AlgoMqttClient(broker, "test", 1, 5000);

        testClient = Mqtt5Client.builder()
            .identifier("testClient")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();
        testClient.connect();

        testClient.publishWith().topic("building/status/elevator/1/committedDirection").payload(String.valueOf(1).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/status/floor/1/button/up").payload(String.valueOf(true).getBytes()).retain(true).send();
        testClient.publishWith().topic("building/status/floor/1/button/down").payload(String.valueOf(true).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/status/elevator/1/floorButton/1").payload(String.valueOf(true).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/control/elevator/1/targetFloor").payload(String.valueOf(1).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/status/elevator/1/floor").payload(String.valueOf(1).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/status/elevator/1/door").payload(String.valueOf(2).getBytes()).retain(true).send();  
        testClient.publishWith().topic("building/status/elevator/1/load/lbs").payload(String.valueOf(100).getBytes()).retain(true).send();
        testClient.publishWith().topic("building/status/elevator/1/speed/feetPerSec").payload(String.valueOf(1).getBytes()).retain(true).send();  
       
        algoMqttClient.run();
        algoMqttClient.stop();

        assertEquals(10, algoMqttClient.getMaxPassengers(1));
    }

    @Test
    public void testMessageArrivedAssert(){     

        algoMqttClient = new AlgoMqttClient(broker, "test", 1, 5000);

        MqttMessage msg = new MqttMessage(String.valueOf(1).getBytes(), 2, false, null);

        assertThrows(MqttError.class, ()->{algoMqttClient.messageArrived("testAssert", msg);});
        assertThrows(MqttError.class, ()->{algoMqttClient.messageArrived("building/status/elevator/1/floorButton/testoutput", msg);});


        PrintStream standardOut = System.out;
        ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outputStreamCaptor));

        algoMqttClient.messageArrived("building/status/elevator/1/testoutput", msg);

        String TestStr = "Elevator algorithm received unhandled topic: " + "building/status/elevator/1/testoutput";
        assertEquals(TestStr, outputStreamCaptor.toString().trim());
        System.setOut(standardOut);

    }
}
