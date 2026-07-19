package com.roomfit.placement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LayoutRepository extends JpaRepository<Layout, Long> {
    Optional<Layout> findFirstByRoomIdAndConfirmedTrueOrderByConfirmedAtDesc(Long roomId);

    List<Layout> findBySourceLayoutIdOrderByIdDesc(Long sourceLayoutId);
}
