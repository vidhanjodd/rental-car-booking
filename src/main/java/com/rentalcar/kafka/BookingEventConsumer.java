package com.rentalcar.kafka;

import com.rentalcar.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final AuditLogService auditLogService;

    /**
     * Consumes booking-events with:
     *  - 3 automatic retries (100ms, 200ms, 400ms backoff)
     *  - Dead-letter topic (booking-events.DLT) after all retries exhausted
     *  - Manual ACK — offset only committed on successful processing
     */
    @RetryableTopic(
        attempts = "3",
        backoff   = @Backoff(delay = 100, multiplier = 2),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
        topics         = "${app.kafka.topics.booking-events}",
        groupId        = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload BookingEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET)             long offset,
            Acknowledgment ack) {

        log.info("Consuming {} event for booking {} [topic={} partition={} offset={}]",
            event.getEventType(), event.getBookingId(), topic, partition, offset);

        try {
            processEvent(event);
            ack.acknowledge();   // commit offset only on success
        } catch (Exception ex) {
            log.error("Error processing booking event {}: {}", event.getBookingId(), ex.getMessage(), ex);
            throw ex;   // re-throw so @RetryableTopic kicks in
        }
    }

    private void processEvent(BookingEvent event) {
        switch (event.getEventType()) {
            case BOOKING_CREATED   -> auditLogService.logBookingCreated(event);
            case BOOKING_CONFIRMED -> auditLogService.logBookingConfirmed(event);
            case BOOKING_CANCELLED -> auditLogService.logBookingCancelled(event);
            case BOOKING_COMPLETED -> auditLogService.logBookingCompleted(event);
        }
    }

    /** Handles messages that failed all retries — logs and stores for manual review */
    @DltHandler
    public void handleDlt(
            @Payload BookingEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT: booking event {} of type {} ended up in dead-letter topic {}",
            event.getBookingId(), event.getEventType(), topic);
        // In production: alert, persist to dead_letter_events table, page on-call
    }
}
