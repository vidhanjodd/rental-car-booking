package com.rentalcar.kafka;

import com.rentalcar.entity.Booking;
import com.rentalcar.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProducer {

    private final KafkaTemplate<String, BookingEvent> kafkaTemplate;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;


    public void publishCreated(Booking booking) {
        publish(buildEvent(booking, BookingEvent.EventType.BOOKING_CREATED, null));
    }

    public void publishConfirmed(Booking booking) {
        publish(buildEvent(booking, BookingEvent.EventType.BOOKING_CONFIRMED, BookingStatus.PENDING));
    }

    public void publishCancelled(Booking booking, BookingStatus previousStatus) {
        publish(buildEvent(booking, BookingEvent.EventType.BOOKING_CANCELLED, previousStatus));
    }

    public void publishCompleted(Booking booking) {
        publish(buildEvent(booking, BookingEvent.EventType.BOOKING_COMPLETED, BookingStatus.CONFIRMED));
    }


    private void publish(BookingEvent event) {
        String key = event.getBookingId().toString();

        CompletableFuture<SendResult<String, BookingEvent>> future =
            kafkaTemplate.send(bookingEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} event for booking {}: {}",
                    event.getEventType(), event.getBookingId(), ex.getMessage());
            } else {
                log.debug("Published {} event for booking {} → partition {} offset {}",
                    event.getEventType(), event.getBookingId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }
        });
    }


    private BookingEvent buildEvent(Booking booking, BookingEvent.EventType type, BookingStatus previousStatus) {
        return BookingEvent.builder()
            .eventType(type)
            .bookingId(booking.getId())
            .userId(booking.getUser().getId())
            .username(booking.getUser().getUsername())
            .carId(booking.getCar().getId())
            .carBrand(booking.getCar().getBrand())
            .carModel(booking.getCar().getModel())
            .startDate(booking.getStartDate())
            .endDate(booking.getEndDate())
            .status(booking.getStatus())
            .previousStatus(previousStatus)
            .totalPrice(booking.getTotalPrice())
            .cancellationReason(booking.getCancellationReason())
            .occurredAt(Instant.now())
            .build();
    }
}
