package at.fhhagenberg.sqelevator.exceptions;

import java.lang.RuntimeException;

public class ElevatorError extends RuntimeException {
    public ElevatorError(String message) {
        super(message);
    }
}
