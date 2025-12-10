package com.multibank.candle.kafka;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.service.CandleAggregationOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidAskEventListener {

    private final CandleAggregationOperation candleAggregationOperation;
    private final CandleConfigProperties properties;

    @KafkaListener(topics = "bid-ask-events", groupId = "candle-aggregator")
    public void onMessage(BidAskEvent event, Acknowledgment ack) {
        log.debug("BidAskEvent from Kafka: {}", event);
        for (TimeFrameConfig tf : properties.getTimeframes()) {
            candleAggregationOperation.createOrUpdateCandle(event, tf);
        }
        ack.acknowledge();
    }
}