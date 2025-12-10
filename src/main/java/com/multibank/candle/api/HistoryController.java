package com.multibank.candle.api;

import com.multibank.candle.api.dto.HistoryResponse;
import com.multibank.candle.domain.Candle;
import com.multibank.candle.service.GetHistoryOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/candles/history")
public class HistoryController implements HistoryApi {

    private final GetHistoryOperation candleService;

    @GetMapping
    public HistoryResponse getHistory(String symbol, String interval, long from, long to) {

        log.info("Requesting history: symbol={}, interval={}, from={}, to={}", symbol, interval, from, to);
        var candles = candleService.getHistory(symbol, interval, from, to);

        if (candles.isEmpty()) {
            return new HistoryResponse("no_data", List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        return getHistoryResponse(candles);
    }

    private static HistoryResponse getHistoryResponse(List<Candle> candles) {
        var t = new ArrayList<Long>();
        var o = new ArrayList<Double>();
        var h = new ArrayList<Double>();
        var l = new ArrayList<Double>();
        var c = new ArrayList<Double>();
        var v = new ArrayList<Long>();

        for (Candle candle : candles) {
            t.add(candle.time());
            o.add(candle.open());
            h.add(candle.high());
            l.add(candle.low());
            c.add(candle.close());
            v.add(candle.volume());
        }

        return new HistoryResponse("ok", t, o, h, l, c, v);
    }
}
