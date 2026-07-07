package com.roomfit.agent;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class AgentContextRepository {

    private final Map<Long, AgentContext> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public AgentContext save(AgentContext context) {
        if (context.getId() == null) {
            context.setId(idGenerator.getAndIncrement());
        }
        store.put(context.getId(), context);
        return context;
    }

    public Optional<AgentContext> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
}
