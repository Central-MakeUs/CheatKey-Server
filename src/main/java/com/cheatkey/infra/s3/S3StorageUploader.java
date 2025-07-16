package com.cheatkey.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3StorageUploader {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucket;

    private static final String REGION = "ap-northeast-2";

    public String upload(File file, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/webp")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, REGION, key);
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public String generatePresignedUrl(String key, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
                .getObjectRequest(getObjectRequest)
                .signatureDuration(duration));

        return presignedRequest.url().toString();
    }
}
