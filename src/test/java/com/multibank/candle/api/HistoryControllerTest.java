package com.multibank.candle.api;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.utils.IntegrationTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.multibank.candle.utils.TestHelpers.candle;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HistoryControllerTest extends IntegrationTestConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private CandleConfigProperties candleConfigProperties;

    @Test
    @DisplayName("GET /history returns candles for symbol+interval within [from,to] ordered by time")
    void shouldReturnHistoryOk() throws Exception {

        // GIVEN
        var symbol = BTC_USD;

        // pick one configured timeframe by name, e.g. "MIN_1"
        var tf = TimeFrameConfig.fromCode(candleConfigProperties.getTimeframes(), "1m");

        var base = 1_000_000L;
        var t1 = base;
        var t2 = base + tf.getSeconds();
        var t3 = base + 2 * tf.getSeconds();
        var tOut = base + 3 * tf.getSeconds(); // outside range

        var c1 = candle(symbol, tf.getCode(), t1, 100.0, 105.0, 99.0, 102.0, 10L);
        var c2 = candle(symbol, tf.getCode(), t2, 102.0, 110.0, 101.0, 108.0, 15L);
        var c3 = candle(symbol, tf.getCode(), t3, 108.0, 112.0, 107.0, 111.0, 20L);
        var cOut = candle(symbol, tf.getCode(), tOut, 111.0, 115.0, 110.0, 114.0, 5L);

        // another symbol → should not be returned
        var otherSymbol = candle("ETH-USD", tf.getCode(), t2, 2000.0, 2010.0, 1995.0, 2005.0, 7L);

        // another timeframe → should not be returned
        var otherTf = candle(symbol, "5m", t2, 300.0, 310.0, 295.0, 305.0, 3L);

        candleRepository.saveAll(List.of(c1, c2, c3, cOut, otherSymbol, otherTf));

        var from = base;
        var to = t3;

        // WHEN / THEN
        mockMvc.perform(get("/api/v1/candles/history")
                        .param("symbol", symbol)
                        .param("interval", tf.getCode())  // "1m"
                        .param("from", String.valueOf(from))
                        .param("to", String.valueOf(to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s").value("ok"))
                // timestamps
                .andExpect(jsonPath("$.t[0]").value((int) t1))
                .andExpect(jsonPath("$.t[1]").value((int) t2))
                .andExpect(jsonPath("$.t[2]").value((int) t3))
                // first candle OHLCV
                .andExpect(jsonPath("$.o[0]").value(100.0))
                .andExpect(jsonPath("$.h[0]").value(105.0))
                .andExpect(jsonPath("$.l[0]").value(99.0))
                .andExpect(jsonPath("$.c[0]").value(102.0))
                .andExpect(jsonPath("$.v[0]").value(10));
    }

    @Test
    @DisplayName("GET /history returns no_data when there are no candles in range")
    void shouldReturnNoDataWhenEmpty() throws Exception {

        // GIVEN: empty repository
        candleRepository.deleteAll();

        // Use a valid timeframe name from config
        TimeFrameConfig tf = candleConfigProperties.getTimeframes().get(0);

        long from = 2_000_000L;
        long to = 2_000_060L;

        // --- WHEN / THEN
        mockMvc.perform(get("/api/v1/candles/history")
                        .param("symbol", BTC_USD)
                        .param("interval", tf.getCode())
                        .param("from", String.valueOf(from))
                        .param("to", String.valueOf(to)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s").value("no_data"))
                .andExpect(jsonPath("$.t").isArray())
                .andExpect(jsonPath("$.t").isEmpty())
                .andExpect(jsonPath("$.o").isEmpty())
                .andExpect(jsonPath("$.h").isEmpty())
                .andExpect(jsonPath("$.l").isEmpty())
                .andExpect(jsonPath("$.c").isEmpty())
                .andExpect(jsonPath("$.v").isEmpty());
    }

    @Test
    @DisplayName("GET /history fails with 400 when interval is invalid")
    void shouldFailOnInvalidInterval() throws Exception {

        // GIVEN
        long from = 1_000_000L;
        long to = 1_000_060L;

        //WHEN / THEN: TimeFrameConfig -> RuntimeException
        mockMvc.perform(get("/api/v1/candles/history")
                        .param("symbol", BTC_USD)
                        .param("interval", "INVALID_INTERVAL")
                        .param("from", String.valueOf(from))
                        .param("to", String.valueOf(to)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("Timeframe does not exists"));
    }

    @Test
    @DisplayName("GET /history fails with 400 when from is greater than to")
    void shouldFailOnInvalidTime() throws Exception {

        // GIVEN
        long from = 1_000_100L;
        long to = 1_000_060L;

        //WHEN / THEN: TimeFrameConfig -> RuntimeException
        mockMvc.perform(get("/api/v1/candles/history")
                        .param("symbol", BTC_USD)
                        .param("interval", "1m")
                        .param("from", String.valueOf(from))
                        .param("to", String.valueOf(to)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("To should be greater than from"));
    }
}