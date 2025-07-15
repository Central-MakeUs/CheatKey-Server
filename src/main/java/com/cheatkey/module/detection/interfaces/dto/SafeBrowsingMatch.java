package com.cheatkey.module.detection.interfaces.dto;

import lombok.Data;

import java.util.List;

@Data
public class SafeBrowsingMatch {
    private String threatType;
    private String platformType;
    private String threatEntryType;
    private Threat threat;
    private ThreatEntryMetadata threatEntryMetadata;

    @Data
    public static class Threat {
        private String url;
    }

    @Data
    public static class ThreatEntryMetadata {
        private List<Entry> entries;

        @Data
        public static class Entry {
            private String key;
            private String value;
        }
    }
}
