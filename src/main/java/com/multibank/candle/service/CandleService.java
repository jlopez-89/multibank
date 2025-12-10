package com.multibank.candle.service;

import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleService {

    private final CandleRepository repository;

    public Optional<CandleEntity> findById(CandleId id) {
        return repository.findById(id);
    }

    @CacheEvict(cacheNames = "history-cache")
    public void save(CandleEntity candle) {
        repository.save(candle);
    }

    @Cacheable(value = "history-cache", key = "#symbol + '|' + #tf.code + '|' + #from + '|' + #to")
    public List<CandleEntity> getHistory(String symbol, TimeFrameConfig tf, long from, long to) {

        return repository
                .findByCandleIdSymbolAndCandleIdTimeframeAndCandleIdTimeBetweenOrderByCandleIdTimeAsc(symbol, tf.getCode(), from, to)
                .stream()
                .toList();
    }

}
