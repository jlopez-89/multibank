package com.multibank.candle.domain;

public record BidAskEvent(String symbol, double bid, double ask, long timestamp) {
}
