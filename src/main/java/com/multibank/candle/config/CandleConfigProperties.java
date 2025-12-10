package com.multibank.candle.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "candle")
@Accessors(chain = true)
public class CandleConfigProperties {

    public Map<String, Double> symbols;
    public List<TimeFrameConfig> timeframes;
}