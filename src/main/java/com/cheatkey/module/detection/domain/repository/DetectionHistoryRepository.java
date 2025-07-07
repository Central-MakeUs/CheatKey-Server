package com.cheatkey.module.detection.domain.repository;

import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetectionHistoryRepository extends JpaRepository<DetectionHistory, Long> {
    List<DetectionHistory> findByUserId(String userId);
}
