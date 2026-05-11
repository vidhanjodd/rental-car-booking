package com.rentalcar.service;

import com.rentalcar.config.RedisConfig;
import com.rentalcar.dto.request.BookingRequest;
import com.rentalcar.dto.response.BookingResponse;
import com.rentalcar.dto.response.PageResponse;
import com.rentalcar.entity.Booking;
import com.rentalcar.entity.Car;
import com.rentalcar.entity.User;
import com.rentalcar.enums.BookingStatus;
import com.rentalcar.enums.CarStatus;
import com.rentalcar.exception.*;
import com.rentalcar.kafka.BookingEventProducer;
import com.rentalcar.repository.BookingRepository;
import com.rentalcar.repository.CarRepository;
import com.rentalcar.repository.UserRepository;
import com.rentalcar.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CarRepository     carRepository;
    private final UserRepository    userRepository;
    private final BookingEventProducer bookingEventProducer;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
        retryFor  = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff   = @Backoff(delay = 100, multiplier = 2)
    )
    @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH, allEntries = true)
    public BookingResponse create(BookingRequest req, UserPrincipal principal) {
        validateDateRange(req.getStartDate(), req.getEndDate());

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));

        Car car = carRepository.findByIdWithPessimisticLock(req.getCarId())
            .orElseThrow(() -> new ResourceNotFoundException("Car", req.getCarId()));

        if (car.getStatus() != CarStatus.AVAILABLE) {
            throw new CarNotAvailableException("Car is currently " + car.getStatus());
        }

        if (bookingRepository.existsOverlappingBooking(car.getId(), req.getStartDate(), req.getEndDate())) {
            throw new BookingDateConflictException();
        }

        long days       = req.getStartDate().until(req.getEndDate()).getDays();
        BigDecimal total = car.getDailyRate().multiply(BigDecimal.valueOf(days));

        Booking booking = Booking.builder()
            .user(user)
            .car(car)
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .status(BookingStatus.PENDING)
            .totalPrice(total)
            .dailyRateSnapshot(car.getDailyRate())
            .pickupLocation(req.getPickupLocation())
            .dropoffLocation(req.getDropoffLocation())
            .notes(req.getNotes())
            .build();

        car.setStatus(CarStatus.BOOKED);
        carRepository.save(car);

        Booking saved = bookingRepository.save(booking);
        bookingEventProducer.publishCreated(saved);
        log.info("Booking created: {} by user {} for car {} [{} → {}]",
            saved.getId(), user.getUsername(), car.getId(), req.getStartDate(), req.getEndDate());

        return toResponse(saved);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse confirm(UUID bookingId, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertOwnerOrAdmin(booking, principal);
        transitionStatus(booking, BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        bookingEventProducer.publishConfirmed(saved);
        log.info("Booking confirmed: {}", bookingId);
        return toResponse(saved);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH, allEntries = true)
    public BookingResponse cancel(UUID bookingId, String reason, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertOwnerOrAdmin(booking, principal);
        BookingStatus previousStatus = booking.getStatus();
        transitionStatus(booking, BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);

        Car car = booking.getCar();
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        Booking saved = bookingRepository.save(booking);
        bookingEventProducer.publishCancelled(saved, previousStatus);
        log.info("Booking cancelled: {} — reason: {}", bookingId, reason);
        return toResponse(saved);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CacheEvict(value = RedisConfig.CACHE_CAR_SEARCH, allEntries = true)
    public BookingResponse complete(UUID bookingId, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        assertAdmin(principal);
        transitionStatus(booking, BookingStatus.COMPLETED);

        Car car = booking.getCar();
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        Booking saved = bookingRepository.save(booking);
        bookingEventProducer.publishCompleted(saved);
        log.info("Booking completed: {}", bookingId);
        return toResponse(saved);
    }


    @Transactional(readOnly = true)
    public BookingResponse getById(UUID id, UserPrincipal principal) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        assertOwnerOrAdmin(booking, principal);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(UserPrincipal principal, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
            bookingRepository.findByUserId(principal.getId(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(BookingStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = status != null
            ? bookingRepository.findByStatus(status, pageable)
            : bookingRepository.findAll(pageable);
        return PageResponse.from(result.map(this::toResponse));
    }


    private void transitionStatus(Booking booking, BookingStatus target) {
        BookingStatus current = booking.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidBookingStateException(current, target);
        }
        booking.setStatus(target);
    }


    private void assertOwnerOrAdmin(Booking booking, UserPrincipal principal) {
        boolean isOwner = booking.getUser().getId().equals(principal.getId());
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You don't have access to this booking");
        }
    }

    private void assertAdmin(UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (!start.isBefore(end)) {
            throw new InvalidBookingStateException("End date must be after start date");
        }
        if (!start.isAfter(LocalDate.now())) {
            throw new InvalidBookingStateException("Start date must be in the future");
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getByIdInternal(UUID id) {
        return toResponse(bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id)));
    }


    public BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
            .id(b.getId())
            .userId(b.getUser().getId())
            .username(b.getUser().getUsername())
            .carId(b.getCar().getId())
            .carBrand(b.getCar().getBrand())
            .carModel(b.getCar().getModel())
            .licensePlate(b.getCar().getLicensePlate())
            .startDate(b.getStartDate())
            .endDate(b.getEndDate())
            .numberOfDays(b.numberOfDays())
            .status(b.getStatus())
            .totalPrice(b.getTotalPrice())
            .dailyRateSnapshot(b.getDailyRateSnapshot())
            .pickupLocation(b.getPickupLocation())
            .dropoffLocation(b.getDropoffLocation())
            .notes(b.getNotes())
            .cancellationReason(b.getCancellationReason())
            .createdAt(b.getCreatedAt())
            .updatedAt(b.getUpdatedAt())
            .build();
    }
}
