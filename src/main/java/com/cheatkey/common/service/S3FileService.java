package com.cheatkey.common.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.file.domain.entity.FileFolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    @Value("${file.upload.max-file-size:5MB}")
    private String maxFileSize;

    @Value("${file.upload.presigned-url-expiration:10}")
    private int presignedUrlExpiration;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "pdf");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final double RESIZE_RATIO = 0.85; // 리사이징 비율
    private static final long RESIZE_THRESHOLD = 5 * 1024 * 1024; // 5MB 이상일 때 리사이징

    /**
     * 파일을 S3에 업로드
     * @param file 업로드할 파일
     * @param folder 파일 폴더 정보
     * @param userId 업로드한 사용자 ID
     * @param isTemp 임시 파일 여부
     * @return S3 key
     */
    public String uploadFile(MultipartFile file, FileFolder folder, Long userId, boolean isTemp) throws ImageException {
        validateFile(file);
        String fileKey = generateFileKey(file, folder, userId, isTemp);
        try {
            InputStream inputStream = processImageIfNeeded(file);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(inputStream.available());
            if (isTemp) {
                metadata.addUserMetadata("is-temp", "true");
                metadata.addUserMetadata("upload-time", String.valueOf(System.currentTimeMillis()));
            }
            PutObjectRequest request = new PutObjectRequest(bucketName, fileKey, inputStream, metadata);
            amazonS3.putObject(request);
            log.info("파일 업로드 성공: fileKey={}, uploadedBy={}, isTemp={}", fileKey, "system", isTemp);
            return fileKey;
        } catch (IOException e) {
            log.error("업로드 실패: fileName={}, reason={}", file.getOriginalFilename(), e.getMessage());
            throw new ImageException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * Presigned URL 생성
     * @param fileKey S3 key
     * @param expirationInMinutes 만료 시간(분)
     * @return Presigned URL
     */
    public URL getPresignedUrl(String fileKey, int expirationInMinutes) throws ImageException {
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileKey);
            request.setExpiration(new java.util.Date(System.currentTimeMillis() + (expirationInMinutes * 60 * 1000)));
            
            URL presignedUrl = amazonS3.generatePresignedUrl(request);
            log.info("Presigned URL 생성: fileKey={}, expiration={}분", fileKey, expirationInMinutes);
            return presignedUrl;
            
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: fileKey={}, reason={}", fileKey, e.getMessage());
            throw new ImageException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }

    /**
     * 기본 만료 시간(10분)으로 Presigned URL 생성
     * @param fileKey S3 key
     * @return Presigned URL
     */
    public URL getPresignedUrl(String fileKey) throws ImageException {
        return getPresignedUrl(fileKey, presignedUrlExpiration);
    }

    /**
     * 파일 삭제
     * @param fileKey S3 key
     */
    public void deleteFile(String fileKey) throws ImageException {
        try {
            if (amazonS3.doesObjectExist(bucketName, fileKey)) {
                amazonS3.deleteObject(bucketName, fileKey);
                log.info("파일 삭제 성공: fileKey={}", fileKey);
            } else {
                log.warn("삭제할 파일이 존재하지 않음: fileKey={}", fileKey);
            }
        } catch (Exception e) {
            log.error("파일 삭제 실패: fileKey={}, reason={}", fileKey, e.getMessage());
            throw new ImageException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    /**
     * S3 임시 파일을 영구 경로로 이동 (copy + delete)
     * @param tempKey 임시 파일 S3 key (예: temp/uuid.jpg)
     * @param permanentKey 영구 파일 S3 key (예: community/uuid.jpg)
     */
    public void moveToPermanent(String tempKey, String permanentKey) throws ImageException {
        try {
            amazonS3.copyObject(bucketName, tempKey, bucketName, permanentKey);
            amazonS3.deleteObject(bucketName, tempKey);
            log.info("S3 파일 이동: {} -> {}", tempKey, permanentKey);
        } catch (Exception e) {
            log.error("S3 파일 이동 실패: {} -> {}, reason={}", tempKey, permanentKey, e.getMessage());
            throw new ImageException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) throws ImageException {
        if (file == null || file.isEmpty()) {
            throw new ImageException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new ImageException(ErrorCode.EMPTY_FILE);
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ImageException(ErrorCode.INVALID_FILE_TYPE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !isValidContentType(contentType)) {
            throw new ImageException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    /**
     * Content-Type 유효성 검사
     */
    private boolean isValidContentType(String contentType) {
        return contentType.startsWith("image/") || contentType.equals("application/pdf");
    }

    /**
     * 이미지 파일인 경우 크기에 따라 리사이징 처리
     */
    private InputStream processImageIfNeeded(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        
        // 이미지 파일이고 5MB 이상인 경우에만 리사이징
        if (isImageFile(contentType) && file.getSize() >= RESIZE_THRESHOLD) {
            log.info("이미지 리사이징 시작: fileName={}, originalSize={}MB", 
                    file.getOriginalFilename(), file.getSize() / (1024 * 1024));
            
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            BufferedImage resizedImage = Thumbnails.of(originalImage)
                    .scale(RESIZE_RATIO)
                    .asBufferedImage();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = getImageFormat(contentType);
            ImageIO.write(resizedImage, format, outputStream);
            
            byte[] resizedBytes = outputStream.toByteArray();
            log.info("이미지 리사이징 완료: fileName={}, resizedSize={}MB", 
                    file.getOriginalFilename(), resizedBytes.length / (1024 * 1024));
            
            return new ByteArrayInputStream(resizedBytes);
        }
        
        return file.getInputStream();
    }

    /**
     * 이미지 파일 여부 확인
     */
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Content-Type에서 이미지 포맷 추출
     */
    private String getImageFormat(String contentType) {
        if (contentType == null) return "jpg";
        
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            default:
                return "jpg";
        }
    }

    /**
     * S3 key 생성
     * 임시: uploads/temp/{folder}/{userId}/{yyyy}/{MM}/{dd}/{uuid}_{originalFilename}
     * 영구: uploads/{folder}/{userId}/{yyyy}/{MM}/{dd}/{uuid}_{originalFilename}
     */
    public String generateFileKey(MultipartFile file, FileFolder folder, Long userId, boolean isTemp) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());
        String uuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        if (isTemp) {
            return String.format("uploads/temp/%s/%d/%s/%s/%s/%s_%s",
                    folder.getFolderName(), userId, year, month, day, uuid, originalFilename);
        } else {
            return String.format("uploads/%s/%d/%s/%s/%s/%s_%s",
                    folder.getFolderName(), userId, year, month, day, uuid, originalFilename);
        }
    }
} 