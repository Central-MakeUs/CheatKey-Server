package com.cheatkey.module.file.domain.service;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.service.S3FileService;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.file.domain.entity.FileFolder;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

    private final S3FileService s3FileService;
    private final FileUploadRepository fileUploadRepository;

    public FileUpload uploadTempFile(MultipartFile file, Long userId) throws ImageException {
        String tempKey = s3FileService.uploadFile(file, FileFolder.COMMUNITY, userId, true);
        URL presignedUrl = s3FileService.getPresignedUrl(tempKey);
        FileUpload fileUpload = FileUpload.builder()
                .userId(userId)
                .originalName(file.getOriginalFilename())
                .s3Key(tempKey)
                .folder(FileFolder.COMMUNITY)
                .size(file.getSize())
                .contentType(file.getContentType())
                .isTemp(true)
                .createdAt(LocalDateTime.now())
                .build();
        return fileUploadRepository.save(fileUpload);
    }

    public URL getPresignedUrl(String fileKey, int expirationInMinutes) throws ImageException {
        return s3FileService.getPresignedUrl(fileKey, expirationInMinutes);
    }

    public URL getPermanentFilePresignedUrl(String fileKey, int expirationInMinutes) throws ImageException {
        // 영구 파일인지 확인
        FileUpload fileUpload = fileUploadRepository.findByS3Key(fileKey)
                .orElseThrow(() -> new ImageException(ErrorCode.FILE_NOT_FOUND));
        
        if (fileUpload.getIsTemp()) {
            throw new ImageException(ErrorCode.FILE_NOT_PERMANENT);
        }
        
        return s3FileService.getPresignedUrl(fileKey, expirationInMinutes);
    }

    public void deleteFile(String fileKey) throws ImageException {
        s3FileService.deleteFile(fileKey);
    }
}
