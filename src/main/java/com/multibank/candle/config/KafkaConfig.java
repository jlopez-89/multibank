package com.multibank.candle.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

import static java.lang.String.valueOf;
import static org.apache.kafka.common.config.TopicConfig.*;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic bidAskEventsTopic(
            @Value("${app.kafka.bid-ask-topic}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .config(RETENTION_MS_CONFIG, valueOf(Duration.ofDays(7).toMillis()))
                .config(CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_DELETE)
                .build();
    }

}
