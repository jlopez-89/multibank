package com.multibank.candle.service;

import com.multibank.candle.repository.CandleRepository;
import com.multibank.candle.repository.entity.CandleId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static com.multibank.candle.utils.IntegrationTestConfig.BTC_USD;
import static com.multibank.candle.utils.TestHelpers.candle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CandleServiceTestIT {

    @Autowired
    private CandleRepository candleRepository;

    @Test
    @DisplayName("findBySymbolAndTimeframeAndTimeBetweenOrderByTimeAsc returns only candles in range ordered by time")
    void shouldFindCandlesInRangeOrderedByTime() {
        var symbol = "BTC-USD";
        var timeframe = "1m";

        var base = 1_000_000L;
        var t1 = base;
        var t2 = base + 60;
        var t3 = base + 120;
        var tOut = base + 180;

        var c1 = candle(symbol, timeframe, t1, 100, 105, 99, 102, 10);
        var c2 = candle(symbol, timeframe, t2, 102, 110, 101, 108, 15);
        var c3 = candle(symbol, timeframe, t3, 108, 112, 107, 111, 20);
        var cOut = candle(symbol, timeframe, tOut, 111, 115, 110, 114, 5);

        var otherSymbol = candle("ETH-USD", timeframe, t2, 2000, 2010, 1995, 2005, 7);
        var otherTimeframe = candle(symbol, "5m", t2, 300, 310, 295, 305, 3);

        candleRepository.saveAll(List.of(c1, c2, c3, cOut, otherSymbol, otherTimeframe));

        var from = base;
        var to = base + 120;

        var result = candleRepository
                .findByCandleIdSymbolAndCandleIdTimeframeAndCandleIdTimeBetweenOrderByCandleIdTimeAsc(
                        symbol, timeframe, from, to);

        assertThat(result)
                .hasSize(3)
                .extracting(e -> e.getCandleId().getTime())
                .containsExactly(t1, t2, t3);
    }

    /**
     * Used to detach one entity so it's no longer tracked by the persistence context,
     * allowing us to simulate two independent updates for optimistic-locking tests.
     */
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Optimistic locking throws exception when saving a stale entity")
    void shouldThrowOptimisticLockingExceptionOnStaleUpdate() {

        // GIVEN: a candle persisted with @Version enabled
        var id = new CandleId(BTC_USD, "1m", 1_000_000L);
        var original = candle(BTC_USD, "1m", 1_000_000L, 100, 105, 99, 102, 1);

        candleRepository.saveAndFlush(original);

        var c1 = candleRepository.findById(id).orElseThrow();
        var c2 = candleRepository.findById(id).orElseThrow();

        // detach removes an entity from the persistence context
        entityManager.detach(c1);
        entityManager.detach(c2);

        // First "transaction": update and save c1 → version in DB increments
        c1.setClose(110);
        candleRepository.saveAndFlush(c1);  // version++ in DB

        // Second "transaction": try to save stale c2 (still has old version)
        c2.setClose(120);

        assertThatThrownBy(() -> candleRepository.saveAndFlush(c2))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }


}