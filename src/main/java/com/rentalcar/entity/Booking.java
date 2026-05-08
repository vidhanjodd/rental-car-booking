package com.rentalcar.entity;

import com.rentalcar.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_bookings_user_id", columnList = "user_id"),
        @Index(name = "idx_bookings_car_id", columnList = "car_id"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_bookings_car_dates", columnList = "car_id, start_date, end_date")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "daily_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRateSnapshot;

    @Column(name = "pickup_location", length = 200)
    private String pickupLocation;

    @Column(name = "dropoff_location", length = 200)
    private String dropoffLocation;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;


    public long numberOfDays() {
        return startDate.until(endDate).getDays();
    }

    public boolean isActive() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }
}
