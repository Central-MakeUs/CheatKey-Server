package com.cheatkey.common.util;

import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ImageException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class FileUtil {

    private static final List<String> IMAGE_FILE_EXTENSIONS = List.of("png", "jpg", "jpeg", "heif");
    private static final int DELETE_RETRY = 3;

    public static File convert(MultipartFile file) throws ImageException {
        String ext = extractExtension(file.getOriginalFilename());
        String safeName = generateSafeFileName(file.getOriginalFilename());
        File tempFile = new File(System.getProperty("java.io.tmpdir"), safeName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
            return tempFile;
        } catch (IOException e) {
            throw new ImageException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static File convertToWebpAndResize(File input, int width, int height) throws ImageException {
        try {
            // @TODO webp 변환 다시 확인하기
            System.out.println("지원 이미지 포맷 목록: " + Arrays.toString(ImageIO.getWriterFormatNames()));

            BufferedImage image = ImageIO.read(input);
            String outputFile = input.getName().replaceFirst("[.][^.]+$", ".webp");
            File result = new File(System.getProperty("java.io.tmpdir"), outputFile);

            Thumbnails.of(image)
                    .size(width, height)
                    .outputFormat("webp")
                    .toFile(result);

            deleteWithRetry(input);
            return result;
        } catch (IOException e) {
            throw new ImageException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static void deleteWithRetry(File file) {
        for (int i = 0; i < DELETE_RETRY; i++) {
            if (file.delete()) return;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }
        log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
    }

    public static String extractExtension(String filename) throws ImageException {
        if (filename == null || !filename.contains(".")) {
            throw new ImageException(ErrorCode.FILE_EXTENSION_FAULT);
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static String generateSafeFileName(String originalName) {
        String base = originalName.substring(0, originalName.lastIndexOf("."));
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String hash = DigestUtils.sha256Hex(originalName + timestamp).substring(0, 8);
        return base + "_" + timestamp + "_" + hash + ext;
    }

    public static String generateS3Key(String folder, String originalFilename) {
        return folder + "/" + generateSafeFileName(originalFilename).replaceAll(" ", "_");
    }

    // S3 Key 추출 유틸
    public static String extractKeyFromUrl(String url) {
        return url.substring(url.indexOf(".com/") + 5);
    }
}
