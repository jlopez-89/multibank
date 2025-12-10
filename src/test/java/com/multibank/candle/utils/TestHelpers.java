package com.multibank.candle.utils;

import com.multibank.candle.config.TimeFrameConfig;
import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;

public class TestHelpers {

    public static TimeFrameConfig oneMinuteTf() {
        TimeFrameConfig tf = new TimeFrameConfig();
        tf.setName("MIN_1");
        tf.setCode("1m");
        tf.setSeconds(60L);
        return tf;
    }

    public static CandleEntity candle(
            String symbol,
            String timeframeCode,
            long timestamp,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
        var id = new CandleId(symbol, timeframeCode, timestamp);
        return new CandleEntity(id, open, high, low, close, volume);
    }

}
