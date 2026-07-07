package com.roomfit.placement;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class LayoutRepository {

    private final Map<Long, Layout> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Layout save(Layout layout) {
        if (layout.getId() == null) {
            layout.setId(idGenerator.getAndIncrement());
        }
        store.put(layout.getId(), layout);
        return layout;
    }

    public Optional<Layout> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}
