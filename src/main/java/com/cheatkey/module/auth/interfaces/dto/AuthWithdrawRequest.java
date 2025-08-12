package com.cheatkey.module.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthWithdrawRequest {
    
    @Schema(description = "탈퇴 사유 코드 (현재는 'WITHDRAWAL_REASON_006'으로 고정)", example = "WITHDRAWAL_REASON_006")
    @NotBlank(message = "탈퇴 사유 코드는 필수입니다")
    @Pattern(regexp = "^WITHDRAWAL_REASON_\\d{3}$", message = "탈퇴 사유 코드 형식이 올바르지 않습니다")
    private String reasonCode = "WITHDRAWAL_REASON_006";
}
