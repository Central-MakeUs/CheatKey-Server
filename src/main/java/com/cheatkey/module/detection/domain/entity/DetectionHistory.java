package com.cheatkey.module.detection.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Builder
@Entity
@Table(name = "t_detection_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DetectionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String inputText;

    private float topScore;

    @Enumerated(EnumType.STRING)
    private DetectionStatus status;

    @CreationTimestamp
    private LocalDateTime detectedAt;

    // Qdrant 검색 결과 중 Top1의 ID (UUID)
    private String matchedCaseId;

    private String detectionType;

    private Long userId;
}

