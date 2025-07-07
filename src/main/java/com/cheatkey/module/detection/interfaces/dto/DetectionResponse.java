package com.cheatkey.module.detection.interfaces.dto;

import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import lombok.Getter;

@Getter
public class DetectionResponse {

    private final DetectionStatus status;
    private final String reason;

    public DetectionResponse(DetectionResult result) {
        this.status = result.status();
        this.reason = result.reason();
    }
}
