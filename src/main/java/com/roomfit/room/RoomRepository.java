package com.roomfit.room;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 기반 Room 저장소.
 * 확정 사항: DB 대신 Map 기반 in-memory 저장소 사용 (데모 규모 고려).
 * 추후 실서비스 확장 시 이 클래스를 JPA Repository 구현체로 교체.
 */
@Repository
public class RoomRepository {

    private final Map<Long, Room> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Room save(Room room) {
        if (room.getId() == null) {
            room.setId(idGenerator.getAndIncrement());
        }
        store.put(room.getId(), room);
        return room;
    }

    public Optional<Room> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public void deleteById(Long id) {
        store.remove(id);
    }

    public List<Room> findAll() {
        return store.values().stream()
                .sorted((first, second) -> Long.compare(first.getId(), second.getId()))
                .toList();
    }

    public List<Room> findBySource(RoomSource source) {
        return store.values().stream()
                .filter(room -> room.getSource() == source)
                .sorted((first, second) -> Long.compare(first.getId(), second.getId()))
                .toList();
    }

    public List<Room> findRecentBySource(RoomSource source, int limit) {
        return store.values().stream()
                .filter(room -> room.getSource() == source)
                .sorted(Comparator.comparing(Room::getCreatedAt).reversed()
                        .thenComparing(Room::getId, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }
}
