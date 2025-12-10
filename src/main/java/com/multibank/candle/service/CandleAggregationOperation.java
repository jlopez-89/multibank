package com.multibank.candle.service;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleAggregationOperation {

    private final CandleService candleService;
    private final CandleConfigProperties properties;

    public void onEvent(BidAskEvent event) {
        for (TimeFrameConfig tf : properties.getTimeframes()) {
            updateCandleWithRetry(event, tf);
        }
    }

    @Retryable(
            retryFor = {OptimisticLockingFailureException.class, OptimisticLockException.class},
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCandleWithRetry(BidAskEvent event, TimeFrameConfig tf) {

        var mid = (event.bid() + event.ask()) / 2.0;
        var candleStart = bucketStart(event.timestamp(), tf.getSeconds());
        var id = new CandleId(event.symbol(), tf.getCode(), candleStart);
        var candle = candleService.findById(id).orElse(null);

        if (candle == null) {
            candle = new CandleEntity(id, mid, mid, mid, mid, 1L);
            log.debug("Creating new candle for symbol={} tf={} time={}", id.getSymbol(), id.getTimeframe(), id.getTime());
        } else {
            var volume = updateCandleEntity(candle, mid);
            log.debug("Updating candle for symbol={} tf={} time={} volume={}",
                    id.getSymbol(), id.getTimeframe(), id.getTime(), volume);
        }

        candleService.save(candle);
    }

    private static long updateCandleEntity(CandleEntity candle, double mid) {
        var open = candle.getOpen();
        var high = Math.max(candle.getHigh(), mid);
        var low = Math.min(candle.getLow(), mid);
        var close = mid;
        var volume = candle.getVolume() + 1;

        candle.setOpen(open);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setClose(close);
        candle.setVolume(volume);
        return volume;
    }

    private long bucketStart(long ts, long tfSeconds) {
        return (ts / tfSeconds) * tfSeconds;
    }
}
