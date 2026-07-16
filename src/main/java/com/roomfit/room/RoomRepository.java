package com.roomfit.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findBySourceOrderByIdAsc(RoomSource source);

    List<Room> findBySourceOrderByCreatedAtDescIdDesc(RoomSource source, Pageable pageable);
}
