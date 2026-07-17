package com.roomfit.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 공유 템플릿만 조회(ownerId가 없는 것) — 게스트별 fork가 샘플 목록에
    // 섞여 나오지 않도록 한다. RoomAccessService 참고.
    List<Room> findBySourceAndOwnerIdIsNullOrderByIdAsc(RoomSource source);

    List<Room> findBySourceAndOwnerIdOrderByCreatedAtDescIdDesc(RoomSource source, String ownerId, Pageable pageable);

    // 같은 게스트가 같은 템플릿을 다시 참조했을 때 새로 fork하지 않고
    // 기존 fork를 재사용하기 위한 조회.
    Optional<Room> findByOwnerIdAndSourceTemplateId(String ownerId, Long sourceTemplateId);
}
