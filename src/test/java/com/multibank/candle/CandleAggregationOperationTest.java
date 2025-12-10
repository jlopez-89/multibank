package com.multibank.candle;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.kafka.BidAskProducer;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleId;
import com.multibank.candle.service.CandleAggregationOperation;
import com.multibank.candle.utils.IntegrationTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

class CandleAggregationOperationTest extends IntegrationTestConfig {

    @Autowired
    private KafkaTemplate<String, BidAskEvent> kafkaTemplate;

    @Autowired
    private CandleAggregationOperation candleAggregationOperation;

    @Autowired
    private BidAskProducer producer;

    @SpyBean
    private CandleRepository candleRepository;

    @Autowired
    private CandleConfigProperties candleConfigProperties;

    @Test
    @DisplayName("Creates a new candle when none exists for symbol+timeframe+bucket")
    void shouldCreateNewCandleWhenNotExists() {

        // GIVEN: a tick event with no existing candle
        var symbol = BTC_USD;
        var bid = 100.0;
        var ask = 102.0;
        var timestamp = 1_000_000L; // epoch seconds

        var event = new BidAskEvent(symbol, bid, ask, timestamp);

        var tf = candleConfigProperties.getTimeframes().get(0);
        var tfSeconds = tf.getSeconds();
        var candleStart = (timestamp / tfSeconds) * tfSeconds;

        var id = new CandleId(symbol, tf.getCode(), candleStart);

        // no candle exists
        assertThat(candleRepository.findByCandleId(id)).isEmpty();

        // WHEN: event is processed
        producer.send(event);

        var mid = (bid + ask) / 2.0;

        // THEN: we wait until the candle appears and validate it
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> candleRepository.findByCandleId(id).isPresent());

        var candle = candleRepository.findByCandleId(id)
                .orElseThrow(() -> new AssertionError("Candle should exist after onEvent"));

        assertThat(candle.getCandleId().getSymbol()).isEqualTo(symbol);
        assertThat(candle.getCandleId().getTimeframe()).isEqualTo(tf.getCode());
        assertThat(candle.getCandleId().getTime()).isEqualTo(candleStart);

        assertThat(candle.getOpen()).isEqualTo(mid);
        assertThat(candle.getHigh()).isEqualTo(mid);
        assertThat(candle.getLow()).isEqualTo(mid);
        assertThat(candle.getClose()).isEqualTo(mid);
        assertThat(candle.getVolume()).isEqualTo(1L);

    }

    @Test
    @DisplayName("Updates OHLC and volume when candle already exists")
    void shouldUpdateExistingCandle() {

        // GIVEN: a first tick creating the candle
        var symbol = BTC_USD;
        var timestamp = 1_000_000L;

        var tf = candleConfigProperties.getTimeframes().get(0);
        var tfSeconds = tf.getSeconds();
        var candleStart = (timestamp / tfSeconds) * tfSeconds;
        var id = new CandleId(symbol, tf.getCode(), candleStart);

        var bid1 = 100.0;
        var ask1 = 102.0;
        var mid1 = (bid1 + ask1) / 2.0;
        var event1 = new BidAskEvent(symbol, bid1, ask1, timestamp);

        producer.send(event1);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> candleRepository.findByCandleId(id).isPresent());

        var afterFirst = candleRepository.findByCandleId(id)
                .orElseThrow(() -> new AssertionError("Candle should exist after first event"));

        assertAll("Initial candle OHLC + volume",
                () -> assertThat(afterFirst.getOpen()).isEqualTo(mid1),
                () -> assertThat(afterFirst.getHigh()).isEqualTo(mid1),
                () -> assertThat(afterFirst.getLow()).isEqualTo(mid1),
                () -> assertThat(afterFirst.getClose()).isEqualTo(mid1),
                () -> assertThat(afterFirst.getVolume()).isEqualTo(1L)
        );

        // WHEN: a second tick arrives with a different price
        var bid2 = 103.0;
        var ask2 = 105.0;
        var mid2 = (bid2 + ask2) / 2.0;
        var event2 = new BidAskEvent(symbol, bid2, ask2, timestamp);

        producer.send(event2);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> candleRepository.findByCandleId(id).get().getVolume() == 2);

        // THEN: the existing candle should be updated
        var afterSecond = candleRepository.findByCandleId(id)
                .orElseThrow(() -> new AssertionError("Candle should still exist after second event"));

        assertThat(afterSecond.getOpen()).isEqualTo(mid1);
        assertThat(afterSecond.getHigh()).isEqualTo(Math.max(mid1, mid2));
        assertThat(afterSecond.getLow()).isEqualTo(Math.min(mid1, mid2));
        assertThat(afterSecond.getClose()).isEqualTo(mid2);
        assertThat(afterSecond.getVolume()).isEqualTo(2L);
    }

}