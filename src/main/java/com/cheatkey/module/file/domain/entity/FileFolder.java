package com.cheatkey.module.file.domain.entity;

public enum FileFolder {
    COMMUNITY("community"),
    PROFILE("profile"),
    BANNER("banner"),
    TEMP("temp"),

    TEST("test-images");

    private final String folderName;
    FileFolder(String folderName) { this.folderName = folderName; }
    public String getFolderName() { return folderName; }
} 