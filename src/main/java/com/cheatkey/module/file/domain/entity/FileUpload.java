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

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "presigned_url", length = 1000)
    private String presignedUrl;

    private String folder;

    private Long size;

    private String contentType;

    private boolean isTemp;

    private LocalDateTime createdAt;

    public void markAsPermanent() {
        this.isTemp = false;
    }
}
