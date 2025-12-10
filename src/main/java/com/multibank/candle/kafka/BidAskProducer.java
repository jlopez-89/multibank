package com.multibank.candle.kafka;


import com.multibank.candle.domain.BidAskEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BidAskProducer {

    private final KafkaTemplate<String, BidAskEvent> kafkaTemplate;

    @Value("${app.kafka.bid-ask-topic}")
    private String topic;

    public void send(BidAskEvent event) {
        kafkaTemplate.send(topic, event.symbol(), event);
    }
}