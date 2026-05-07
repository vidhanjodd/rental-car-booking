package com.rentalcar.repository;

import com.rentalcar.entity.Car;
import com.rentalcar.enums.CarCategory;
import com.rentalcar.enums.CarStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {

    /**
     * Find cars available for the requested date range and city.
     * Excludes cars that have PENDING or CONFIRMED bookings overlapping the window.
     */
    @Query("""
        SELECT c FROM Car c
        WHERE c.status = 'AVAILABLE'
          AND (:city IS NULL OR LOWER(c.city) = LOWER(:city))
          AND (:category IS NULL OR c.category = :category)
          AND (:minRate IS NULL OR c.dailyRate >= :minRate)
          AND (:maxRate IS NULL OR c.dailyRate <= :maxRate)
          AND NOT EXISTS (
              SELECT 1 FROM Booking b
              WHERE b.car = c
                AND b.status IN ('PENDING', 'CONFIRMED')
                AND b.startDate < :endDate
                AND b.endDate   > :startDate
          )
        ORDER BY c.dailyRate ASC
        """)
    Page<Car> findAvailableCars(
        @Param("city")      String city,
        @Param("category")  CarCategory category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate,
        @Param("minRate")   BigDecimal minRate,
        @Param("maxRate")   BigDecimal maxRate,
        Pageable pageable
    );

    /**
     * Pessimistic write lock — used in booking confirmation
     * to prevent two threads from booking the same car simultaneously.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdWithPessimisticLock(@Param("id") UUID id);

    Optional<Car> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    Page<Car> findByStatus(CarStatus status, Pageable pageable);

    Page<Car> findByCity(String city, Pageable pageable);

    @Query("SELECT DISTINCT c.city FROM Car c WHERE c.deleted = false ORDER BY c.city")
    java.util.List<String> findAllCities();
}
