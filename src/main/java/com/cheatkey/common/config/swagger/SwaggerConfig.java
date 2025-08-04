package com.cheatkey.common.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("CheatKey API")
                .version("1.0.0")
                .description("CheatKey 서비스의 API 문서입니다.");

        // ErrorResponse 스키마 정의
        Schema<?> errorResponseSchema = new Schema<>()
                .type("object")
                .addProperty("code", new StringSchema().description("에러 코드"))
                .addProperty("message", new StringSchema().description("에러 메시지"));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. (Bearer 제외)"))
                        .addSchemas("ErrorResponse", errorResponseSchema))
                .addServersItem(new Server().url("https://api.cheatskey.com").description("운영 환경 (IP 접근)"));
    }
}
