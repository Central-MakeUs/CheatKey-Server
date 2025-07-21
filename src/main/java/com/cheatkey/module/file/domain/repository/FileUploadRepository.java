package com.cheatkey.module.file.domain.repository;

import com.cheatkey.module.file.domain.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByUserIdAndIsTempTrue(Long userId);
    
    Optional<FileUpload> findByS3Key(String s3Key);
}
