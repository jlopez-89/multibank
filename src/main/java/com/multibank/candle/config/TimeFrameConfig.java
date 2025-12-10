package com.multibank.candle.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeFrameConfig {
    private String name;
    private String code;
    private long seconds;

    public static TimeFrameConfig fromCode(List<TimeFrameConfig> timeFrameConfigList, String code) {
        return timeFrameConfigList
                .stream()
                .filter(tf -> tf.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Timeframe does not exists"));
    }
}