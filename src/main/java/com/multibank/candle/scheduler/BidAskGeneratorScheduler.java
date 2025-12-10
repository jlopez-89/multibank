package com.multibank.candle.scheduler;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.kafka.BidAskProducer;
import com.multibank.candle.service.PriceSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BidAskGeneratorScheduler {

    private final BidAskProducer bidAskProducer;
    private final CandleConfigProperties properties;


    @Scheduled(fixedDelay = 1000)
    public void generateTicks() {

        var priceSimulator = new PriceSimulator();
        long start = System.currentTimeMillis();
        long nowEpochSeconds = Instant.now().getEpochSecond();

        for (String symbol : properties.getSymbols().keySet()) {

            double mid = priceSimulator.nextPrice(symbol, properties.getSymbols().get(symbol));

            // little fixed spread
            double spread = mid * 0.0005;
            double bid = mid - spread / 2;
            double ask = mid + spread / 2;

            var event = new BidAskEvent(symbol, bid, ask, nowEpochSeconds);

            bidAskProducer.send(event);

            log.debug("Generated tick symbol={} mid={} bid={} ask={}", symbol, mid, bid, ask);
        }

        log.debug("completed in  " + (System.currentTimeMillis() - start) + " ms");
    }
}