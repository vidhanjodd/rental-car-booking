package com.rentalcar.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidBookingStateException extends RuntimeException {
    public InvalidBookingStateException(String message) { super(message); }
    public InvalidBookingStateException(Object from, Object to) {
        super("Cannot transition booking from " + from + " to " + to);
    }
}
