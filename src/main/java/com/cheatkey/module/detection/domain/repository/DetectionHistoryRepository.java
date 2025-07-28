package com.cheatkey.module.detection.domain.repository;

import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DetectionHistoryRepository extends JpaRepository<DetectionHistory, Long> {
    Page<DetectionHistory> findByUserIdAndDetectedAtAfterOrderByDetectedAtDesc(Long userId, LocalDateTime startDate, Pageable pageable);
}
