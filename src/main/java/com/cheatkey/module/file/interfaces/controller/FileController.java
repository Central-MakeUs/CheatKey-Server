package com.cheatkey.module.file.interfaces.controller;

import com.cheatkey.common.exception.ImageException;
import com.cheatkey.module.file.domain.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam MultipartFile file,
                                         @RequestParam String folder,
                                         @RequestParam Long userId) throws ImageException {
        String url = fileService.uploadAndSaveTemp(file, folder, userId);
        return ResponseEntity.ok(url);
    }
}

