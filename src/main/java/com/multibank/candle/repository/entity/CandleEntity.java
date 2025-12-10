package com.multibank.candle.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "candles",
        indexes = {
                @Index(name = "idx_candles_symbol_tf_time", columnList = "symbol,timeframe,time")
        }
)
public class CandleEntity {

    @EmbeddedId
    private CandleId candleId;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "open", nullable = false)
    private double open;

    @Column(name = "high", nullable = false)
    private double high;

    @Column(name = "low", nullable = false)
    private double low;

    @Column(name = "close", nullable = false)
    private double close;

    @Column(name = "volume", nullable = false)
    private long volume;

    public CandleEntity(CandleId candleId, double open, double high, double low, double close, long volume) {
        this.candleId = candleId;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}