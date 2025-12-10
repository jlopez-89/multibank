package com.multibank.candle.scheduler;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.kafka.BidAskProducer;
import com.multibank.candle.service.PriceSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidAskGeneratorSchedulerTest {

    @Mock
    private BidAskProducer bidAskProducer;

    private CandleConfigProperties properties;
    private BidAskGeneratorScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new CandleConfigProperties();
        var symbols = new LinkedHashMap<String, Double>();
        symbols.put("BTC-USD", 10000d);
        symbols.put("ETH-USD", 4000d);
        properties.setSymbols(symbols);

        scheduler = new BidAskGeneratorScheduler(bidAskProducer, properties);
    }

    @Test
    @DisplayName("generateTicks sends one tick per configured symbol")
    void shouldGenerateOneTickPerSymbol() {
        scheduler.generateTicks();

        verify(bidAskProducer, times(2)).send(any(BidAskEvent.class));
    }
}