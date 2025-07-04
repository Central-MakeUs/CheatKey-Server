package com.cheatkey.module.home.interfaces.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag(name = "Home", description = "홈화면(메인) API")
public class HomeController {

    @Operation(summary = "홈화면(매인)")
    @GetMapping("/home")
    public String home() {

        // @TODO API 수정하기
        return "홈화면(메인)";
    }
}
