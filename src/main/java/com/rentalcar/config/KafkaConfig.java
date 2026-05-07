package com.rentalcar.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;

    @Value("${app.kafka.topics.booking-events-dlt}")
    private String bookingEventsDltTopic;

    @Value("${app.kafka.topics.car-status-events}")
    private String carStatusEventsTopic;

    @Bean
    public NewTopic bookingEventsTopic() {
        return TopicBuilder.name(bookingEventsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic bookingEventsDltTopic() {
        return TopicBuilder.name(bookingEventsDltTopic)
            .partitions(1)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic carStatusEventsTopic() {
        return TopicBuilder.name(carStatusEventsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public RecordMessageConverter messageConverter() {
        return new StringJsonMessageConverter();
    }
}
