package com.multibank.candle.api.dto;

import java.util.List;

public record HistoryResponse(
        String s, List<Long> t, List<Double> o, List<Double> h, List<Double> l, List<Double> c, List<Long> v) {
}