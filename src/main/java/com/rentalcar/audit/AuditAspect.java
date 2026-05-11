package com.rentalcar.audit;

import com.rentalcar.dto.response.BookingResponse;
import com.rentalcar.dto.response.CarResponse;
import com.rentalcar.security.UserPrincipal;
import com.rentalcar.service.BookingService;
import com.rentalcar.service.CarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final CarService carService;
    private final BookingService bookingService;

    @Pointcut("execution(public * com.rentalcar.service.BookingService.create(..))")
    private void bookingCreate() {}

    @Pointcut("execution(public * com.rentalcar.service.BookingService.confirm(..))")
    private void bookingConfirm() {}

    @Pointcut("execution(public * com.rentalcar.service.BookingService.cancel(..))")
    private void bookingCancel() {}

    @Pointcut("execution(public * com.rentalcar.service.BookingService.complete(..))")
    private void bookingComplete() {}

    @Pointcut("execution(public * com.rentalcar.service.CarService.updateStatus(..))")
    private void carStatusUpdate() {}

    @Pointcut("execution(public * com.rentalcar.service.CarService.update(..))")
    private void carUpdate() {}

    @Pointcut("execution(public * com.rentalcar.service.CarService.create(..))")
    private void carCreate() {}

    @Pointcut("execution(public * com.rentalcar.service.CarService.delete(..))")
    private void carDelete() {}


    @Around("bookingCreate() || bookingConfirm() || bookingCancel() || bookingComplete()")
    public Object auditBookingOperation(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        String actor  = resolveActor();

        String oldStatus = null;
        Object[] args = pjp.getArgs();

        if (!method.equals("create") && args.length > 0 && args[0] instanceof UUID bookingId) {
            try {
                BookingResponse existing = bookingService.getByIdInternal(bookingId);
                oldStatus = existing.getStatus().name();
            } catch (Exception e) {
                log.warn("Could not fetch old booking status for audit: {}", e.getMessage());
            }
        }

        Object result = pjp.proceed();

        if (result instanceof BookingResponse booking) {
            auditLogService.log(
                    "Booking",
                    booking.getId(),
                    method.toUpperCase(),
                    oldStatus,
                    booking.getStatus().name(),
                    actor,
                    "BookingService." + method + " executed by " + actor
            );
        }
        return result;
    }

    @Around("carStatusUpdate()")
    public Object auditCarStatusUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args    = pjp.getArgs();
        UUID carId       = args.length > 0 ? (UUID) args[0] : null;
        String newStatus = args.length > 1 ? args[1].toString() : "UNKNOWN";
        String actor     = resolveActor();

        String oldStatus = null;
        if (carId != null) {
            try {
                CarResponse existing = carService.getById(carId);
                oldStatus = existing.getStatus().name();
            } catch (Exception e) {
                log.warn("Could not fetch old car status for audit: {}", e.getMessage());
            }
        }

        Object result = pjp.proceed();

        auditLogService.log(
                "Car", carId, "STATUS_CHANGE",
                oldStatus,
                newStatus,
                actor,
                "Car status changed to " + newStatus + " by " + actor
        );
        return result;
    }


    @Around("carUpdate() || carCreate() || carDelete()")
    public Object auditCarOperation(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        String actor  = resolveActor();
        Object[] args = pjp.getArgs();

        String oldValue = null;
        if (method.equals("update") && args.length > 0 && args[0] instanceof UUID carId) {
            try {
                CarResponse existing = carService.getById(carId);
                oldValue = existing.toString();
            } catch (Exception e) {
                log.warn("Could not fetch old car state for audit: {}", e.getMessage());
            }
        }

        Object result = pjp.proceed();

        String newValue = null;
        String entityId = null;

        if (result instanceof CarResponse car) {
            newValue = car.toString();
            entityId = car.getId().toString();
        } else if (method.equals("delete") && args.length > 0) {
            entityId = args[0].toString();
        }

        auditLogService.log(
                "Car",
                entityId != null ? UUID.fromString(entityId) : null,
                method.toUpperCase(),
                oldValue,
                newValue,
                actor,
                "CarService." + method + " executed by " + actor
        );

        return result;
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        return "SYSTEM";
    }
}
