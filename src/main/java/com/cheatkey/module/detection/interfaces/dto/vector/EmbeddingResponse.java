package com.cheatkey.module.detection.interfaces.dto.vector;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EmbeddingResponse {
    private List<Float> vector;
}
