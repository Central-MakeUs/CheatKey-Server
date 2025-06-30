package com.cheatkey.module.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class AuthRegisterRequest {

    @NotBlank
    @Size(min = 2, max = 5)
    private String nickname;

    @NotBlank
    private String ageCode;

    @NotBlank
    private String genderCode;

    private List<String> tradeMethodCode;
    private List<String> tradeItemCode;
}
