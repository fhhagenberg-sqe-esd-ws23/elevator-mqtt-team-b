# sqelevator-proj
Group assignment SQElevator

## MQTT Topics
Common topics:  
```
    building/info/numberOfElevators
    building/info/numberOfFloors
    building/info/floorHeight/feet
    building/info/systemClockTick
```

Elevator topics:  
```
    building/status/elevator/{id}/committedDirection
    building/status/elevator/{id}/acceleration/feetPerSqSec
    building/status/elevator/{id}/floorButton/{id}
    building/status/elevator/{id}/door
    building/status/elevator/{id}/floor
    building/status/elevator/{id}/position/feet
    building/status/elevator/{id}/speed/feetPerSec
    building/status/elevator/{id}/load/lbs
    building/info/elevator/{id}/floorService/{id}
    building/info/elevator/{id}/targetFloor
    building/info/elevator/{id}/maxPassengers
    building/control/elevator/{id}/committedDirection
    building/control/elevator/{id}/floorService/{id}
    building/control/elevator/{id}/targetFloor
```

Floor topics:  
```
    building/status/floor/{id}/button/up
    building/status/floor/{id}/button/down
```

## Doc:

```sh
# generate Javadoc
mvn javadoc:javadoc
```
The documentation can be found under `target/site/apidocs`
