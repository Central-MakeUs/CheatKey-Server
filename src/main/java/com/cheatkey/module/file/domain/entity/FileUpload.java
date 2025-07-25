package com.cheatkey.module.file.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_file_upload")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String originalName;

    @Column(name = "s3_key")
    private String s3Key;

    @Enumerated(EnumType.STRING)
    private FileFolder folder;

    private Long size;

    private String contentType;

    private boolean isTemp;

    private LocalDateTime createdAt;

    public void markAsPermanent() {
        this.isTemp = false;
    }

    public boolean getIsTemp() {
        return isTemp;
    }

    public void updateToPermanent(String newS3Key) {
        this.s3Key = newS3Key;
        this.isTemp = false;
    }
}
