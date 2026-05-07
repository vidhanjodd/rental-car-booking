package com.rentalcar.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.CONFLICT)
public class CarNotAvailableException extends RuntimeException {
    public CarNotAvailableException() { super("Car is not available for the requested dates"); }
    public CarNotAvailableException(String message) { super(message); }
}
