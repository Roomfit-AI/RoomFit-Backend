package com.roomfit.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findBySourceOrderByIdAsc(RoomSource source);

    List<Room> findBySourceOrderByCreatedAtDescIdDesc(RoomSource source, Pageable pageable);

    @Query("""
            select r from Room r
            where r.source = :source
              and (r.clientScope = :clientScope or (:legacy = true and r.clientScope is null))
            order by r.createdAt desc, r.id desc
            """)
    List<Room> findAccessibleBySourceOrderByCreatedAtDescIdDesc(@Param("source") RoomSource source,
                                                                 @Param("clientScope") String clientScope,
                                                                 @Param("legacy") boolean legacy,
                                                                 Pageable pageable);
}
