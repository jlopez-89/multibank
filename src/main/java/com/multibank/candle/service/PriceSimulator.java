package com.multibank.candle.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PriceSimulator {

    private final Random random = new Random();
    private final Map<String, Double> lastPriceBySymbol = new ConcurrentHashMap<>();

    public double nextPrice(String symbol, Double price) {
        var last = lastPriceBySymbol.computeIfAbsent(symbol, s -> price);
        var maxMove = last * 0.001;
        var delta = (random.nextDouble() * 2 - 1) * maxMove;
        var next = Math.max(1.0, last + delta);
        lastPriceBySymbol.put(symbol, next);
        return next;
    }

}