package com.rentalcar.repository;

import com.rentalcar.entity.Booking;
import com.rentalcar.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Check for any ACTIVE bookings on this car that overlap the requested window.
     * Used BEFORE creating a new booking to detect conflicts.
     *
     * Two date ranges [A,B] and [C,D] overlap iff:  A < D  &&  C < B
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.car.id = :carId
          AND b.status IN ('PENDING', 'CONFIRMED')
          AND b.startDate < :endDate
          AND b.endDate   > :startDate
        """)
    boolean existsOverlappingBooking(
        @Param("carId")     UUID carId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate
    );

    /**
     * Same overlap check, but excludes a specific booking id — used when updating.
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.car.id = :carId
          AND b.id <> :excludeId
          AND b.status IN ('PENDING', 'CONFIRMED')
          AND b.startDate < :endDate
          AND b.endDate   > :startDate
        """)
    boolean existsOverlappingBookingExcluding(
        @Param("carId")     UUID carId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate")   LocalDate endDate,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Pessimistic read lock for status transitions — serialises concurrent updates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") UUID id);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    Page<Booking> findByCarId(UUID carId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    List<Booking> findByCarIdAndStatusIn(UUID carId, List<BookingStatus> statuses);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.car
        JOIN FETCH b.user
        WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
          AND b.status = :status
        ORDER BY b.createdAt DESC
        """)
    Page<Booking> findByUserIdAndStatus(
        @Param("userId") UUID userId,
        @Param("status") BookingStatus status,
        Pageable pageable
    );
}
