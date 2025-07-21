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

    public String uploadFile(MultipartFile multipartFile, FileFolder folder, Long userId) throws ImageException {
        String fileKey = s3FileService.uploadFile(multipartFile, folder, true);
        URL presignedUrl = s3FileService.getPresignedUrl(fileKey);
        
        FileUpload fileUpload = FileUpload.builder()
                .userId(userId)
                .originalName(multipartFile.getOriginalFilename())
                .s3Key(fileKey)
                .presignedUrl(presignedUrl.toString())
                .folder(folder)
                .size(multipartFile.getSize())
                .contentType(multipartFile.getContentType())
                .isTemp(true)
                .createdAt(LocalDateTime.now())
                .build();

        fileUploadRepository.save(fileUpload);
        
        return presignedUrl.toString();
    }

    /**
     * 게시글 생성 후 파일 처리
     */
    @Transactional
    public void handleImagesAfterPostCreate(List<String> usedUrls, Long userId) throws ImageException {
        List<FileUpload> tempFiles = fileUploadRepository.findByUserIdAndIsTempTrue(userId);

        for (FileUpload file : tempFiles) {
            if (usedUrls.contains(file.getPresignedUrl())) {
                // 사용된 파일은 영구화
                file.markAsPermanent();
            } else {
                // 사용되지 않은 파일은 S3에서만 삭제, DB에는 그대로(isTemp=true) 남김
                s3FileService.deleteFile(file.getS3Key());
                // fileUploadRepository.delete(file); // 삭제하지 않음
            }
        }
    }

    public URL getPresignedUrl(String fileKey, int expirationInMinutes) throws ImageException {
        return s3FileService.getPresignedUrl(fileKey, expirationInMinutes);
    }

    public URL getPermanentFilePresignedUrl(String fileKey, int expirationInMinutes) throws ImageException {
        // 영구 파일인지 확인
        FileUpload fileUpload = fileUploadRepository.findByS3Key(fileKey)
                .orElseThrow(() -> new ImageException(ErrorCode.FILE_NOT_FOUND));
        
        if (fileUpload.isTemp()) {
            throw new ImageException(ErrorCode.FILE_NOT_PERMANENT);
        }
        
        return s3FileService.getPresignedUrl(fileKey, expirationInMinutes);
    }

    public void deleteFile(String fileKey) throws ImageException {
        s3FileService.deleteFile(fileKey);
    }
}
