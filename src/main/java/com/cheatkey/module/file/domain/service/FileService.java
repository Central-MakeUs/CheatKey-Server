package com.cheatkey.module.file.domain.service;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.common.util.FileUtil;
import com.cheatkey.infra.s3.S3StorageUploader;
import com.cheatkey.module.file.domain.entity.FileUpload;
import com.cheatkey.module.file.domain.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FileService {

    private final S3StorageUploader s3Uploader;
    private final FileUploadRepository fileUploadRepository;

    public String uploadAndSaveTemp(MultipartFile multipartFile, String folder, Long userId) throws ImageException {
        File local = FileUtil.convert(multipartFile);
        File webp = FileUtil.convertToWebpAndResize(local, 720, 140);
        String key = FileUtil.generateS3Key(folder, webp.getName());
        String url = s3Uploader.upload(webp, key);

        fileUploadRepository.save(FileUpload.builder()
                .userId(userId)
                .originalName(multipartFile.getOriginalFilename())
                .storedName(webp.getName())
                .url(url)
                .folder(folder)
                .size(multipartFile.getSize())
                .contentType(multipartFile.getContentType())
                .isTemp(true)
                .createdAt(LocalDateTime.now())
                .build());

        return url;
    }

    @Transactional
    public void handleImagesAfterPostCreate(List<String> usedUrls, Long userId) {
        List<FileUpload> tempFiles = fileUploadRepository.findByUserIdAndIsTempTrue(userId);

        for (FileUpload file : tempFiles) {
            if (usedUrls.contains(file.getUrl())) {
                file.markAsPermanent();
            } else {
                s3Uploader.delete(FileUtil.extractKeyFromUrl(file.getUrl()));
                fileUploadRepository.delete(file);
            }
        }
    }
}
