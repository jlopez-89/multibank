package com.multibank.candle.service;

import com.multibank.candle.config.CandleConfigProperties;
import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.domain.Candle;
import com.multibank.candle.repository.entity.CandleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetHistoryOperation {

    private final CandleService service;
    private final CandleConfigProperties properties;

    @Transactional(readOnly = true)
    public List<Candle> getHistory(String symbol, String interval, long from, long to) {

        var tf = TimeFrameConfig.fromCode(properties.getTimeframes(), interval);
        validateFromTo(from, to);
        return service
                .getHistory(symbol, tf, from, to)
                .stream()
                .map(this::buildCandle)
                .toList();
    }

    private static void validateFromTo(long from, long to) {
        if (from >= to) {
            throw new RuntimeException("To should be greater than from");
        }
    }

    private Candle buildCandle(CandleEntity e) {
        return new Candle(e.getCandleId().getTime(), e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVolume());
    }
}
