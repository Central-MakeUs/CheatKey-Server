package com.cheatkey.module.file.domain.repository;

import com.cheatkey.module.file.domain.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByUserIdAndIsTempTrue(Long userId);
}
