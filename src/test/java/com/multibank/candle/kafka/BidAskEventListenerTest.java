package com.multibank.candle.kafka;


import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.utils.IntegrationTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@Testcontainers
class BidAskEventListenerTest extends IntegrationTestConfig {

    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "candle-aggregator");
        registry.add("spring.kafka.listener.ack-mode", () -> "manual_immediate");
    }

    @Autowired
    private BidAskProducer producer;

    @Autowired
    private CandleRepository candleRepository;

    @DirtiesContext
    @ParameterizedTest
    @CsvSource(value = {"6000:100", "3000:50", "600:10", "60:1"}, delimiter = ':')
    @DisplayName("Brutal load test: BidAskEventListener handles thousands of events via Kafka")
    void shouldHandleBrutalLoadThroughKafka(int totalEvents, int expectedCandles) throws Exception {
        // GIVEN
        double baseBid = 100.0;
        double baseAsk = 102.0;

        long now = Instant.now().getEpochSecond();
        long baseTs = now - (now % 60);

        for (int i = 0; i < totalEvents; i++) {
            long ts = baseTs + i;
            double bid = baseBid + i * 0.11;
            double ask = baseAsk + i * 0.11;

            var event = new BidAskEvent(BTC_USD, bid, ask, ts);
            producer.send(event);
            // Test containers can't process high volume and need to give some time to process the events on the right way
            Thread.sleep(1);
        }

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    List<CandleEntity> all1 = candleRepository.findAll();
                    System.out.println(all1.size());
                    return all1.size() == expectedCandles;
                });
    }
}