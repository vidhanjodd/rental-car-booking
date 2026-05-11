package com.rentalcar.entity;

import com.rentalcar.enums.CarCategory;
import com.rentalcar.enums.CarStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(
    name = "cars",
    indexes = {
        @Index(name = "idx_cars_license_plate", columnList = "license_plate", unique = true),
        @Index(name = "idx_cars_status", columnList = "status"),
        @Index(name = "idx_cars_city_status", columnList = "city, status")
    }
)
@SQLDelete(sql = "UPDATE cars SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Car extends BaseEntity {

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CarCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CarStatus status = CarStatus.AVAILABLE;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Column(name = "transmission", length = 20)
    private String transmission;   // AUTOMATIC | MANUAL

    @Column(name = "fuel_type", length = 20)
    private String fuelType;       // PETROL | DIESEL | ELECTRIC | HYBRID

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Optimistic locking — prevents lost updates under concurrent access.
     * JPA increments this on every UPDATE; a stale-state write throws
     * ObjectOptimisticLockingFailureException.
     */
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings;
}
