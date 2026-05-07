package com.rentalcar.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class BookingRequest {

    @NotNull(message = "Car ID is required")
    private UUID carId;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 200)
    private String pickupLocation;

    @Size(max = 200)
    private String dropoffLocation;

    @Size(max = 1000)
    private String notes;
}
