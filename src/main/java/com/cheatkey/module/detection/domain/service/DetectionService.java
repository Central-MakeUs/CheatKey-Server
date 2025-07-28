package com.cheatkey.module.detection.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.mapper.DetectionMapper;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.mypage.interfaces.dto.DetectionDetailResponse;
import com.cheatkey.module.mypage.interfaces.dto.DetectionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cheatkey.module.detection.domain.entity.DetectionPeriod;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectionService {

    private final DetectionHistoryRepository detectionHistoryRepository;
    private final DetectionMapper detectionMapper;

    public Page<DetectionHistoryResponse> getDetectionHistory(Long userId, DetectionPeriod period, Pageable pageable) {
        LocalDateTime startDate = period.getStartDate();
        Page<DetectionHistory> histories = detectionHistoryRepository.findByUserIdAndDetectedAtAfterOrderByDetectedAtDesc(userId, startDate, pageable);
        return histories.map(detectionMapper::toDetectionHistoryResponse);
    }

    public DetectionDetailResponse getDetectionDetail(Long userId, Long detectionId) {
        DetectionHistory history = detectionHistoryRepository.findById(detectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.DETECTION_HISTORY_NOT_FOUND));
        
        if (!history.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.DETECTION_HISTORY_ACCESS_DENIED);
        }
        
        return detectionMapper.toDetectionDetailResponse(history);
    }
} 