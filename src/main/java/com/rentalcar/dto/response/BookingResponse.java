package com.rentalcar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {
    private UUID          id;
    private UUID          userId;
    private String        username;
    private UUID          carId;
    private String        carBrand;
    private String        carModel;
    private String        licensePlate;
    private LocalDate     startDate;
    private LocalDate     endDate;
    private long          numberOfDays;
    private BookingStatus status;
    private BigDecimal    totalPrice;
    private BigDecimal    dailyRateSnapshot;
    private String        pickupLocation;
    private String        dropoffLocation;
    private String        notes;
    private String        cancellationReason;
    private Instant       createdAt;
    private Instant       updatedAt;
}
