package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;

import com.hivemq.client.internal.mqtt.message.connect.connack.MqttConnAck;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import at.fhhagenberg.sqelevator.exceptions.ElevatorError;
import at.fhhagenberg.sqelevator.exceptions.MqttError;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.hivemq.HiveMQContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;



@Testcontainers
public class ElevatorMqttAdapterTest {

    @Mock private IElevator elevatorIface;
    //@Mock private MqttAsyncClient mqttClientMock;
    private ElevatorMqttAdapter elevatorMqttAdapter;

    private Mqtt5AsyncClient asyncClient;
    private Mqtt5BlockingClient testClient;


    @Container
    public HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));

    @BeforeEach
    void setUp() throws MqttException {
        testClient = Mqtt5Client.builder()
            .identifier("testClient")
            .serverPort(container.getMqttPort())
            .serverHost(container.getHost())
            .buildBlocking();

        testClient.connect(); 

        MockitoAnnotations.initMocks(this);
        String Host = "tcp://" + container.getHost() + ":" + container.getMqttPort(); //mqtt.eclipseprojects.io:1883";
        elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, Host, "ElevatorTest", 2, 1000);
    }

    @Test
    void testConstructor() {
        assertThrows(IllegalArgumentException.class , ()->{
            elevatorMqttAdapter = new ElevatorMqttAdapter(elevatorIface, "tcp://mqtttest 4711", "ElevatorTest", 2, 1000);
        });
    }
     
    @Test 
    void testConnectToBroker()  {
        elevatorMqttAdapter.connectToBroker();
    }

    @Test
    void testReadStatesPublishStates() throws RemoteException, InterruptedException {
        elevatorMqttAdapter.connectToBroker();
        
        when(elevatorIface.getElevatorNum()).thenReturn(2);
        when(elevatorIface.getElevatorFloor(1)).thenReturn(1);
        when(elevatorIface.getElevatorAccel(1)).thenReturn(15);
        when(elevatorIface.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorIface.getElevatorPosition(1)).thenReturn(1);
        when(elevatorIface.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorIface.getElevatorWeight(1)).thenReturn(10);
        when(elevatorIface.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorIface.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorIface.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorIface.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorIface.getFloorNum()).thenReturn(5);
        when(elevatorIface.getFloorHeight()).thenReturn(3);
        when(elevatorIface.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorIface.getTarget(1)).thenReturn(5);
        when(elevatorIface.getClockTick()).thenReturn(1000L);
        when(elevatorIface.getCommittedDirection(1)).thenReturn(1);   
        
        
        testClient.subscribeWith().topicFilter("building/status/elevator/1/committedDirection").send();
        

        Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        Mqtt5Publish received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/committedDirection", received.getTopic().toString());
        assertEquals("1", payload); 

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/committedDirection").send();
        testClient.subscribeWith().topicFilter("building/status/elevator/1/acceleration/feetPerSqSec").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/acceleration/feetPerSqSec", received.getTopic().toString());
        assertEquals("15", payload);

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/acceleration/feetPerSqSec").send();
        testClient.subscribeWith().topicFilter("building/status/elevator/1/door").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/door", received.getTopic().toString());
        assertEquals("2", payload); 

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/door").send();
        testClient.subscribeWith().topicFilter("building/status/elevator/1/floor").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/floor", received.getTopic().toString());
        assertEquals("1", payload);

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/floor").send();
        // testClient.subscribeWith().topicFilter("building/status/elevator/1/position/feet").send();

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishState();

        // received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/status/elevator/1/position/feet", received.getTopic().toString());
        // assertEquals("1", payload);

        // testClient.unsubscribeWith().topicFilter("building/status/elevator/1/position/feet").send();
        testClient.subscribeWith().topicFilter("building/status/elevator/1/speed/feetPerSec").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/speed/feetPerSec", received.getTopic().toString());
        assertEquals("5", payload); 

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/speed/feetPerSec").send();
        testClient.subscribeWith().topicFilter("building/status/elevator/1/load/lbs").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/elevator/1/load/lbs", received.getTopic().toString());
        assertEquals("10", payload);

        testClient.unsubscribeWith().topicFilter("building/status/elevator/1/load/lbs").send();
        testClient.subscribeWith().topicFilter("building/info/elevator/1/floorService/1").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/info/elevator/1/floorService/1", received.getTopic().toString());
        assertEquals("true", payload);

        testClient.unsubscribeWith().topicFilter("building/info/elevator/1/floorService/1").send();
        testClient.subscribeWith().topicFilter("building/info/elevator/1/targetFloor").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/info/elevator/1/targetFloor", received.getTopic().toString());
        assertEquals("5", payload); 

        testClient.unsubscribeWith().topicFilter("building/info/elevator/1/targetFloor").send();

        testClient.subscribeWith().topicFilter("building/status/floor/1/button/up").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/floor/1/button/up", received.getTopic().toString());
        assertEquals("false", payload);

        testClient.unsubscribeWith().topicFilter("building/status/floor/1/button/up").send();
        testClient.subscribeWith().topicFilter("building/status/floor/1/button/down").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishState();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/status/floor/1/button/down", received.getTopic().toString());
        assertEquals("true", payload); 

        testClient.unsubscribeWith().topicFilter("building/status/floor/1/button/down").send();
        testClient.subscribeWith().topicFilter("building/info/systemClockTick").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishRetainedTopics();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/info/systemClockTick", received.getTopic().toString());
        assertEquals("1000", payload); 

    }

    /**
     * Test Publish Retained Topics, but only two at a time possible
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Test
    void testPublishRetainedTopics() throws RemoteException, InterruptedException {
        elevatorMqttAdapter.connectToBroker();
        
        when(elevatorIface.getElevatorNum()).thenReturn(2);
        when(elevatorIface.getElevatorFloor(1)).thenReturn(1);
        when(elevatorIface.getElevatorAccel(1)).thenReturn(15);
        when(elevatorIface.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorIface.getElevatorPosition(1)).thenReturn(1);
        when(elevatorIface.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorIface.getElevatorWeight(1)).thenReturn(10);
        when(elevatorIface.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorIface.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorIface.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorIface.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorIface.getFloorNum()).thenReturn(5);
        when(elevatorIface.getFloorHeight()).thenReturn(3);
        when(elevatorIface.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorIface.getTarget(1)).thenReturn(5);
        when(elevatorIface.getClockTick()).thenReturn(1000L);
        when(elevatorIface.getCommittedDirection(1)).thenReturn(1);   
        
        
        testClient.subscribeWith().topicFilter("building/info/systemClockTick").send();

        Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishRetainedTopics();

        Mqtt5Publish received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/info/systemClockTick", received.getTopic().toString());
        assertEquals("1000", payload);    
        
        testClient.unsubscribeWith().topicFilter("building/info/systemClockTick").send();
        // testClient.subscribeWith().topicFilter("building/info/floorHeight/feet").send();

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishRetainedTopics();

        // received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/info/floorHeight/feet", received.getTopic().toString());
        // assertEquals("3", payload); 

        // testClient.unsubscribeWith().topicFilter("building/info/floorHeight/feet").send();
        // testClient.subscribeWith().topicFilter("building/info/numberOfFloors").send();

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishRetainedTopics();

        // received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/info/numberOfFloors", received.getTopic().toString());
        // assertEquals("5", payload); 

        // testClient.unsubscribeWith().topicFilter("building/info/numberOfFloors").send();
        // testClient.subscribeWith().topicFilter("building/info/elevator/1/maxPassengers").send();

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishRetainedTopics();

        // received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/info/elevator/1/maxPassengers", received.getTopic().toString());
        // assertEquals("5", payload);

        // testClient.unsubscribeWith().topicFilter("building/info/elevator/1/maxPassengers").send();
        testClient.subscribeWith().topicFilter("building/info/numberOfElevators").send();

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.publishRetainedTopics();

        received = publishes.receive();

        assertNotNull(received.getPayloadAsBytes());
        payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        assertEquals("building/info/numberOfElevators", received.getTopic().toString());
        assertEquals("2", payload);  
    }

    @Test
    void testPublishRetainedTopicsErrorNotConnected(){
        assertThrows(MqttError.class, ()->{
            elevatorMqttAdapter.publishRetainedTopics();
        });
    }

    @Test
    void testPublishRetainedTopicsErrorWhilePublishing() throws RemoteException{
        
        when(elevatorIface.getElevatorNum()).thenReturn(2);
        when(elevatorIface.getElevatorFloor(1)).thenReturn(1);
        when(elevatorIface.getElevatorAccel(1)).thenReturn(15);
        when(elevatorIface.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorIface.getElevatorPosition(1)).thenReturn(1);
        when(elevatorIface.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorIface.getElevatorWeight(1)).thenReturn(10);
        when(elevatorIface.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorIface.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorIface.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorIface.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorIface.getFloorNum()).thenReturn(5);
        when(elevatorIface.getFloorHeight()).thenReturn(3);
        when(elevatorIface.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorIface.getTarget(1)).thenReturn(5);
        when(elevatorIface.getClockTick()).thenReturn(1000L);
        when(elevatorIface.getCommittedDirection(1)).thenReturn(1);

        //

        assertThrows(NullPointerException.class, ()->{
            
            //testClient.subscribeWith().topicFilter("building/info/numberOfElevators").send();
            elevatorMqttAdapter.connectToBroker();
            //elevatorMqttAdapter.readStates();
            //elevatorMqttAdapter.subscribeToController();
            elevatorMqttAdapter.publishRetainedTopics();
        });
    }

    /**
     * Test if Subscribe to Controller works
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Test 
    void testSubscribeToController() throws RemoteException, InterruptedException  {
        elevatorMqttAdapter.connectToBroker();
        when(elevatorIface.getElevatorNum()).thenReturn(2);
        when(elevatorIface.getElevatorFloor(1)).thenReturn(1);
        when(elevatorIface.getElevatorAccel(1)).thenReturn(15);
        when(elevatorIface.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorIface.getElevatorPosition(1)).thenReturn(1);
        when(elevatorIface.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorIface.getElevatorWeight(1)).thenReturn(10);
        when(elevatorIface.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorIface.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorIface.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorIface.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorIface.getFloorNum()).thenReturn(5);
        when(elevatorIface.getFloorHeight()).thenReturn(3);
        when(elevatorIface.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorIface.getTarget(1)).thenReturn(5);
        when(elevatorIface.getClockTick()).thenReturn(1000L);
        when(elevatorIface.getCommittedDirection(1)).thenReturn(1);

        elevatorMqttAdapter.readStates();
        elevatorMqttAdapter.subscribeToController();
        
        // Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishRetainedTopics();

        // Mqtt5Publish received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/info/numberOfElevators", received.getTopic().toString());
        // assertEquals("2", payload); 

    }

    /**
     * Test if Subscribe to Controller works
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Test 
    void testRun() throws RemoteException, InterruptedException  {
        
        when(elevatorIface.getElevatorNum()).thenReturn(2);
        when(elevatorIface.getElevatorFloor(1)).thenReturn(1);
        when(elevatorIface.getElevatorAccel(1)).thenReturn(15);
        when(elevatorIface.getElevatorDoorStatus(1)).thenReturn(2);
        when(elevatorIface.getElevatorPosition(1)).thenReturn(1);
        when(elevatorIface.getElevatorSpeed(1)).thenReturn(5);
        when(elevatorIface.getElevatorWeight(1)).thenReturn(10);
        when(elevatorIface.getElevatorCapacity(1)).thenReturn(5);
        when(elevatorIface.getElevatorButton(1, 1)).thenReturn(true);

        when(elevatorIface.getFloorButtonDown(1)).thenReturn(true);
        when(elevatorIface.getFloorButtonUp(1)).thenReturn(false);
        when(elevatorIface.getFloorNum()).thenReturn(5);
        when(elevatorIface.getFloorHeight()).thenReturn(3);
        when(elevatorIface.getServicesFloors(1, 1)).thenReturn(true);

        when(elevatorIface.getTarget(1)).thenReturn(5);
        when(elevatorIface.getClockTick()).thenReturn(1000L);
        when(elevatorIface.getCommittedDirection(1)).thenReturn(1);

        MqttError MqttError = assertThrows(MqttError.class, ()->{
            elevatorMqttAdapter.run();
        });
        //assertEquals("MqttError", MqttError.getMessage());
        // Mqtt5BlockingClient.Mqtt5Publishes publishes = testClient.publishes(MqttGlobalPublishFilter.ALL); 

        // elevatorMqttAdapter.readStates();
        // elevatorMqttAdapter.publishRetainedTopics();

        // Mqtt5Publish received = publishes.receive();

        // assertNotNull(received.getPayloadAsBytes());
        // String payload = new String(received.getPayloadAsBytes(), StandardCharsets.UTF_8);
        // assertEquals("building/info/numberOfElevators", received.getTopic().toString());
        // assertEquals("2", payload); 

    }

    @Test
    void testSubscribeToControllerAssert() throws RemoteException {
        assertThrows(MqttError.class, ()->{
            elevatorMqttAdapter.subscribeToController();
        });
    }


    /**
     * Test if the adapter can handle a second connection to the broker without crashing
     */
    @Test 
    void testConnectTwiceToBroker()  {
        elevatorMqttAdapter.connectToBroker();
        elevatorMqttAdapter.connectToBroker();
    }

}