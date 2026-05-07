package com.rentalcar.kafka;

import com.rentalcar.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {

    public enum EventType {
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        BOOKING_COMPLETED
    }

    private EventType     eventType;
    private UUID          bookingId;
    private UUID          userId;
    private String        username;
    private UUID          carId;
    private String        carBrand;
    private String        carModel;
    private LocalDate     startDate;
    private LocalDate     endDate;
    private BookingStatus status;
    private BookingStatus previousStatus;
    private BigDecimal    totalPrice;
    private String        cancellationReason;
    private Instant       occurredAt;
}
