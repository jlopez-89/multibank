package com.multibank.candle.service;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.domain.Candle;
import com.multibank.candle.utils.TestHelpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHistoryOperationTest {

    private static final String BTC_USD = "BTC-USD";

    @Mock
    private CandleService candleService;

    private GetHistoryOperation getHistoryOperation;

    @BeforeEach
    void setUp() {
        CandleConfigProperties properties = new CandleConfigProperties();
        properties.setTimeframes(List.of(new TimeFrameConfig("MIN_1", "1m", 60L)));
        getHistoryOperation = new GetHistoryOperation(candleService, properties);
    }

    @Test
    @DisplayName("Returns candles in [from,to] for symbol+timeframe ordered by ascending time")
    void shouldReturnCandlesInRangeOrderedByTime() {
        // --- GIVEN: three candles inside the time range ---
        var tf = TestHelpers.oneMinuteTf();
        var symbol = BTC_USD;

        var base = 1_000_000L;
        var t1 = base;
        var t2 = base + 60;
        var t3 = base + 120;

        var c1 = TestHelpers.candle(symbol, tf.getCode(), t1, 100, 105, 99, 102, 10);
        var c2 = TestHelpers.candle(symbol, tf.getCode(), t2, 102, 110, 101, 108, 15);
        var c3 = TestHelpers.candle(symbol, tf.getCode(), t3, 108, 112, 107, 111, 20);

        var from = base;
        var to = base + 120;

        // We mock the service to return EXACTLY the candles inside [from, to],
        // already filtered and ordered, as the repository would do.
        when(candleService.getHistory(eq(symbol), eq(tf), eq(from), eq(to)))
                .thenReturn(List.of(c1, c2, c3));

        // WHEN ---
        var result = getHistoryOperation.getHistory(symbol, tf.getCode(), from, to);

        // THEN: only candles in range, ordered by time ---
        assertThat(result)
                .hasSize(3)
                .extracting(Candle::time)
                .containsExactly(t1, t2, t3);

        var r1 = result.get(0);
        assertThat(r1.open()).isEqualTo(100);
        assertThat(r1.high()).isEqualTo(105);
        assertThat(r1.low()).isEqualTo(99);
        assertThat(r1.close()).isEqualTo(102);
        assertThat(r1.volume()).isEqualTo(10);

        var r3 = result.get(2);
        assertThat(r3.time()).isEqualTo(t3);
        assertThat(r3.close()).isEqualTo(111);
    }


    @Test
    @DisplayName("Returns empty list when no candles exist in range")
    void shouldReturnEmptyListWhenNoCandlesInRange() {
        var tf = TestHelpers.oneMinuteTf();
        long from = 2_000_000L;
        long to = 2_000_060L;

        when(candleService.getHistory(eq(BTC_USD), eq(tf), eq(from), eq(to))).thenReturn(List.of());

        List<Candle> result = getHistoryOperation.getHistory(BTC_USD, tf.getCode(), from, to);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Throws when 'from' is greater or equal to 'to'")
    void shouldThrowWhenFromIsGreaterOrEqualToTo() {
        var tf = TestHelpers.oneMinuteTf();
        long from = 2_000_060L;
        long to = 2_000_060L; // igual → debe petar

        assertThatThrownBy(() -> getHistoryOperation.getHistory(BTC_USD, tf.getCode(), from, to))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("To should be greater than from");
    }

    @Test
    @DisplayName("Throws when Timeframe is not configured")
    void shouldThrowWhenTimeFrameIsNotConfigured() {
        var tf = new TimeFrameConfig("ABC","ABC",1L);
        long from = 2_000_060L;
        long to = 2_000_160L;

        assertThatThrownBy(() -> getHistoryOperation.getHistory(BTC_USD, tf.getCode(), from, to))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timeframe does not exists");
    }
}