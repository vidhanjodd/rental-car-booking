package com.rentalcar.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.CONFLICT)
public class BookingDateConflictException extends RuntimeException {
    public BookingDateConflictException() { super("A booking already exists for this car in the requested date range"); }
}
