package com.multibank.candle.service;

import com.multibank.candle.domain.BidAskEvent;
import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;
import com.multibank.candle.utils.IntegrationTestConfig;
import com.multibank.candle.utils.TestHelpers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

class CandleAggregationOperationRetryTest extends IntegrationTestConfig {

    @Autowired
    private CandleAggregationOperation candleAggregationOperation;

    @MockBean
    private CandleRepository candleRepository;

    @Test
    @DisplayName("updateCandleWithRetry retries when save() throws OptimisticLockingFailureException")
    void shouldRetryOnOptimisticLock() {

        // GIVEN - A tick event
        var event = new BidAskEvent(BTC_USD, 100.0, 102.0, 1_000_000L);

        when(candleRepository.findByCandleId(any(CandleId.class)))
                .thenReturn(Optional.empty());

        AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            if (counter.getAndIncrement() == 0) {
                throw new OptimisticLockingFailureException("forced");
            }
            return invocation.getArgument(0, CandleEntity.class);
        }).when(candleRepository).save(any(CandleEntity.class));

        // WHEN
        candleAggregationOperation.createOrUpdateCandle(event, TestHelpers.oneMinuteTf());

        // THEN
        verify(candleRepository, times(2)).save(any(CandleEntity.class));
    }
}