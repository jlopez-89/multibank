package com.multibank.candle.utils;

import com.multibank.candle.repository.entity.CandleEntity;
import com.multibank.candle.repository.entity.CandleId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryCandleStore {

    private final Map<CandleId, CandleEntity> store = new ConcurrentHashMap<>();

    public Optional<CandleEntity> findById(CandleId id) {
        return Optional.ofNullable(store.get(id));
    }

    public void save(CandleEntity c) {
        store.put(c.getCandleId(), c);
    }

    public int size() {
        return store.size();
    }

    public CandleEntity single() {
        assertThat(store).hasSize(1);
        return store.values().iterator().next();
    }

    public Collection<CandleEntity> all() {
        return store.values();
    }

}
