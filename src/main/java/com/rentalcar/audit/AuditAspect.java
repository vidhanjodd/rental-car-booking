package com.rentalcar.audit;

import com.rentalcar.dto.response.BookingResponse;
import com.rentalcar.security.UserPrincipal;
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

/**
 * Cross-cutting audit concern: intercepts all public BookingService methods
 * and writes an audit record after successful execution.
 *
 * Uses @Around so we capture both input args and the returned value.
 * Audit write is async (inside AuditLogService) so it never slows down the main call.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

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

    @Around("bookingCreate() || bookingConfirm() || bookingCancel() || bookingComplete()")
    public Object auditBookingOperation(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getName();
        String actor  = resolveActor();

        Object result = pjp.proceed();   // execute the real method

        if (result instanceof BookingResponse booking) {
            auditLogService.log(
                "Booking",
                booking.getId(),
                method.toUpperCase(),
                null,
                booking.getStatus().name(),
                actor,
                "BookingService." + method + " executed by " + actor
            );
        }
        return result;
    }

    @Around("carStatusUpdate()")
    public Object auditCarStatusUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args  = pjp.getArgs();
        UUID carId     = args.length > 0 ? (UUID) args[0] : null;
        String newStatus = args.length > 1 ? args[1].toString() : "UNKNOWN";
        String actor   = resolveActor();

        Object result = pjp.proceed();

        auditLogService.log(
            "Car", carId, "STATUS_CHANGE",
            null, newStatus, actor,
            "Car status changed to " + newStatus + " by " + actor
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
