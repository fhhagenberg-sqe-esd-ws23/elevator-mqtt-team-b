package at.fhhagenberg.sqelevator.Algorithm;

import at.fhhagenberg.sqelevator.exceptions.InvalidArgError;

public class Building {
    public Elevator[] elevators;

    public Building(int nrOfElevators, int nrOfFloors, AlgoMqttClient client) {
        if (nrOfElevators <= 0 || nrOfFloors <= 0) {
            throw new InvalidArgError("Number of elevators and number of floors must be a positive integer");
        }
        this.elevators = new Elevator[nrOfElevators];

        for (int i = 0; i < elevators.length; i++) {             
            elevators[i] = new Elevator(i, nrOfFloors, client.getMaxPassengers(i), client);                                 
        }
    }

    /**
     * Sets the 'best' next target for each elevator in the building. Must be called in a process loop.
     */
    public void updateElevatorTargets() {
        for (int i = 0; i < elevators.length; i++) {  
            elevators[i].updateTarget();
        }
    }
}
