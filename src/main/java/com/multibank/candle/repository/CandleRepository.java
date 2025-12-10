package com.multibank.candle.repository;

import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandleRepository extends JpaRepository<CandleEntity, CandleId> {

    List<CandleEntity> findByCandleIdSymbolAndCandleIdTimeframeAndCandleIdTimeBetweenOrderByCandleIdTimeAsc(
            String symbol,
            String timeframe,
            long from,
            long to
    );

    Optional<CandleEntity> findByCandleId(CandleId id);
}
