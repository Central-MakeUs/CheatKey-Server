package com.cheatkey.module.detection.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class SafeBrowsingResponse {
    private List<SafeBrowsingMatch> matches;
}
