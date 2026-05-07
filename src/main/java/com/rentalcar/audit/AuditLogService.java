package com.rentalcar.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalcar.entity.AuditLog;
import com.rentalcar.kafka.BookingEvent;
import com.rentalcar.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper       objectMapper;

    // ── Kafka-driven audit entries ─────────────────────────────────────────

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookingCreated(BookingEvent event) {
        save(AuditLog.builder()
            .entityType("Booking")
            .entityId(event.getBookingId())
            .action("BOOKING_CREATED")
            .newValue(toJson(event))
            .actor(event.getUsername())
            .details("Booking created for car " + event.getCarBrand() + " " + event.getCarModel()
                + " from " + event.getStartDate() + " to " + event.getEndDate())
            .createdAt(Instant.now())
            .build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookingConfirmed(BookingEvent event) {
        save(AuditLog.builder()
            .entityType("Booking")
            .entityId(event.getBookingId())
            .action("STATUS_CHANGE")
            .oldValue("PENDING")
            .newValue("CONFIRMED")
            .actor(event.getUsername())
            .details("Booking confirmed")
            .createdAt(Instant.now())
            .build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookingCancelled(BookingEvent event) {
        save(AuditLog.builder()
            .entityType("Booking")
            .entityId(event.getBookingId())
            .action("STATUS_CHANGE")
            .oldValue(event.getPreviousStatus() != null ? event.getPreviousStatus().name() : "UNKNOWN")
            .newValue("CANCELLED")
            .actor(event.getUsername())
            .details("Booking cancelled. Reason: " + event.getCancellationReason())
            .createdAt(Instant.now())
            .build());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBookingCompleted(BookingEvent event) {
        save(AuditLog.builder()
            .entityType("Booking")
            .entityId(event.getBookingId())
            .action("STATUS_CHANGE")
            .oldValue("CONFIRMED")
            .newValue("COMPLETED")
            .actor(event.getUsername())
            .details("Booking completed")
            .createdAt(Instant.now())
            .build());
    }

    // ── Manual audit entries (called from AOP aspect) ──────────────────────

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action,
                    String oldValue, String newValue, String actor, String details) {
        save(AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .oldValue(oldValue)
            .newValue(newValue)
            .actor(actor)
            .details(details)
            .createdAt(Instant.now())
            .build());
    }

    private void save(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to save audit log: {}", ex.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
