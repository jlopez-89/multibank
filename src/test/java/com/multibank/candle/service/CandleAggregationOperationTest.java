package com.multibank.candle.service;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;
import com.multibank.candle.utils.InMemoryCandleStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.multibank.candle.utils.IntegrationTestConfig.BTC_USD;
import static com.multibank.candle.utils.TestHelpers.candle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandleAggregationOperationTest {

    @Mock
    private CandleService candleService;

    @Mock
    private CandleRepository candleRepository;

    private CandleAggregationOperation candleAggregationOperation;

    @BeforeEach
    void setUp() {
        CandleConfigProperties properties = new CandleConfigProperties();
        properties.setTimeframes(List.of(new TimeFrameConfig("MIN_1", "1m", 60L)));
        candleAggregationOperation = new CandleAggregationOperation(candleService, properties);
    }

    @Test
    @DisplayName("Creates a new candle when none exists for that symbol + timeframe + bucket")
    void shouldCreateNewCandleWhenNotExists() {
        // GIVEN
        var symbol = BTC_USD;
        var bid = 100.0;
        var ask = 102.0;
        var ts = 1_100_000L;

        var event = new BidAskEvent(symbol, bid, ask, ts);

        // There's not entity before
        when(candleService.findById(any(CandleId.class))).thenReturn(Optional.empty());

        // WHEN
        candleAggregationOperation.onEvent(event);

        // THEN
        var captor = ArgumentCaptor.forClass(CandleEntity.class);
        verify(candleService).save(captor.capture());

        var saved = captor.getValue();

        var mid = (bid + ask) / 2.0;

        assertThat(saved.getCandleId().getSymbol()).isEqualTo(symbol);
        assertThat(saved.getOpen()).isEqualTo(mid);
        assertThat(saved.getHigh()).isEqualTo(mid);
        assertThat(saved.getLow()).isEqualTo(mid);
        assertThat(saved.getClose()).isEqualTo(mid);
        assertThat(saved.getVolume()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Updates an existing candle (OHLC and volume)")
    void shouldUpdateExistingCandle() {

        // GIVEN
        var symbol = BTC_USD;
        var bid = 100.0;
        var ask = 102.0;
        var ts = 1_100_000L;

        var event = new BidAskEvent(symbol, bid, ask, ts);
        var mid = (bid + ask) / 2.0;

        var existing = candle(symbol, "1m", (ts / 60) * 60, 100.5, 101.5, 100.0, 101.0, 10L);

        when(candleService.findById(any(CandleId.class))).thenReturn(Optional.of(existing));

        // WHEN
        candleAggregationOperation.onEvent(event);

        // THEN
        ArgumentCaptor<CandleEntity> captor = ArgumentCaptor.forClass(CandleEntity.class);
        verify(candleService).save(captor.capture());

        var updated = captor.getValue();

        assertThat(updated.getOpen()).isEqualTo(100.5);
        assertThat(updated.getHigh()).isEqualTo(101.5);
        assertThat(updated.getLow()).isEqualTo(100.0);
        assertThat(updated.getClose()).isEqualTo(mid);
        assertThat(updated.getVolume()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Many ticks in the same bucket → correct volume and OHLC")
    void shouldAggregateManyTicksInSameBucket() {
        var symbol = "BTC-USD";

        var baseTs = alignToMinute(1_100_000L);

        var memStore = setupInMemoryStore();

        int totalEvents = 60; // 0..59 → just 1m bucket

        for (int i = 0; i < totalEvents; i++) {
            var bid = 100.0 + i;
            var ask = 102.0 + i;
            var ts = baseTs + i; // all in the same minute

            var event = new BidAskEvent(symbol, bid, ask, ts);
            candleAggregationOperation.onEvent(event);
        }

        var candle = memStore.single();
        assertThat(candle.getVolume()).isEqualTo(totalEvents);

        var firstMid = (100.0 + 102.0) / 2.0; // 101.0
        var lastBid = 100.0 + (totalEvents - 1);  // 159
        var lastAsk = 102.0 + (totalEvents - 1);  // 161
        var lastMid = (lastBid + lastAsk) / 2.0;  // 160.0

        assertThat(candle.getOpen()).isEqualTo(firstMid);
        assertThat(candle.getClose()).isEqualTo(lastMid);
        assertThat(candle.getHigh()).isGreaterThanOrEqualTo(lastMid);
        assertThat(candle.getLow()).isLessThanOrEqualTo(firstMid);
    }

    @Test
    @DisplayName("Events in multiple consecutive buckets → multiple candles")
    void shouldCreateCandlesForMultipleBuckets() {

        // GIVEN
        var baseTs = alignToMinute(1_100_000L);
        var memStore = setupInMemoryStore();

        // three different buckets (1 per minute)
        int buckets = 3;
        int eventsPerBucket = 10;

        for (int b = 0; b < buckets; b++) {
            var bucketStart = baseTs + b * 60;
            for (int i = 0; i < eventsPerBucket; i++) {
                var ts = bucketStart + i;
                var bid = 100 + b;
                var ask = 102 + b;
                var event = new BidAskEvent(BTC_USD, bid, ask, ts);
                // WHEN
                candleAggregationOperation.onEvent(event);
            }
        }

        //THEN
        assertThat(memStore.size()).isEqualTo(buckets);
        memStore.all().forEach(c -> assertThat(c.getVolume()).isEqualTo(eventsPerBucket));
    }

    @Test
    @DisplayName("Out-of-order events in the same bucket → consistent OHLC/volume")
    void shouldHandleOutOfOrderEventsInSameBucket() {

        //GIVEN
        var symbol = BTC_USD;
        var baseTs = alignToMinute(1_100_000L);

        var memStore = setupInMemoryStore();

        var e1 = new BidAskEvent(symbol, 100.0, 102.0, baseTs + 30);
        var e2 = new BidAskEvent(symbol, 90.0, 92.0, baseTs + 10);
        var e3 = new BidAskEvent(symbol, 110.0, 112.0, baseTs + 50);

        // WHEN
        candleAggregationOperation.onEvent(e1);
        candleAggregationOperation.onEvent(e3);
        candleAggregationOperation.onEvent(e2);

        //THEN
        var c = memStore.single();

        assertThat(c.getOpen()).isEqualTo((100.0 + 102.0) / 2.0);  // 101
        assertThat(c.getHigh()).isEqualTo((110.0 + 112.0) / 2.0);  // 111
        assertThat(c.getLow()).isEqualTo((90.0 + 92.0) / 2.0);     // 91
        assertThat(c.getClose()).isEqualTo((90.0 + 92.0) / 2.0);   // 91
        assertThat(c.getVolume()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Single event with multiple timeframes → one candle per timeframe")
    void shouldCreateOneCandlePerConfiguredTimeframe() {

        // Reconfigure properties
        var multiProps = new CandleConfigProperties();
        multiProps.setTimeframes(List.of(
                new TimeFrameConfig("SEC_1", "1s", 1L),
                new TimeFrameConfig("MIN_1", "1m", 60L),
                new TimeFrameConfig("MIN_5", "5m", 300L)
        ));
        candleAggregationOperation = new CandleAggregationOperation(candleService, multiProps);

        var ts = alignToMinute(1_100_000L);
        var bid = 100.0;
        var ask = 102.0;
        var mid = (bid + ask) / 2.0;

        var memStore = setupInMemoryStore();

        // WHEN: 1 event 3 timeframes
        var event = new BidAskEvent(BTC_USD, bid, ask, ts);
        candleAggregationOperation.onEvent(event);

        // THEN: candle per timeframe
        assertThat(memStore.size()).isEqualTo(3);

        var byTimeframe = memStore
                .all()
                .stream()
                .collect(Collectors.toMap(c -> c.getCandleId().getTimeframe(), Function.identity()));

        assertThat(byTimeframe.keySet()).containsExactlyInAnyOrder("1s", "1m", "5m");

        byTimeframe.values().forEach(c -> {
            assertThat(c.getOpen()).isEqualTo(mid);
            assertThat(c.getHigh()).isEqualTo(mid);
            assertThat(c.getLow()).isEqualTo(mid);
            assertThat(c.getClose()).isEqualTo(mid);
            assertThat(c.getVolume()).isEqualTo(1L);
        });
    }

    private static long alignToMinute(long ts) {
        return ts - (ts % 60);
    }

    private InMemoryCandleStore setupInMemoryStore() {
        var memStore = new InMemoryCandleStore();

        when(candleService.findById(any(CandleId.class))).thenAnswer(inv -> {
            CandleId id = inv.getArgument(0);
            return memStore.findById(id);
        });

        doAnswer(inv -> {
            CandleEntity c = inv.getArgument(0);
            memStore.save(c);
            return null;
        }).when(candleService).save(any(CandleEntity.class));

        return memStore;
    }
}