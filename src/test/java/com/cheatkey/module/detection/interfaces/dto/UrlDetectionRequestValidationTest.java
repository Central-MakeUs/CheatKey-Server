package com.cheatkey.module.detection.interfaces.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlDetectionRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void 올바른_URL_정규식_통과() {
        UrlDetectionRequest req = new UrlDetectionRequest("https://phishing-site.com");
        Set<ConstraintViolation<UrlDetectionRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void http로_시작하지않는_URL_정규식_실패() {
        UrlDetectionRequest req = new UrlDetectionRequest("ftp://phishing-site.com");
        Set<ConstraintViolation<UrlDetectionRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void 도메인_없는_URL_정규식_실패() {
        UrlDetectionRequest req = new UrlDetectionRequest("http://");
        Set<ConstraintViolation<UrlDetectionRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void 특수문자_이상한_URL_정규식_실패() {
        UrlDetectionRequest req = new UrlDetectionRequest("http://<script>alert(1)</script>");
        Set<ConstraintViolation<UrlDetectionRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void 길이_초과_URL_실패() {
        String longUrl = "http://" + "a".repeat(95) + ".com";
        UrlDetectionRequest req = new UrlDetectionRequest(longUrl);
        Set<ConstraintViolation<UrlDetectionRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }
} 