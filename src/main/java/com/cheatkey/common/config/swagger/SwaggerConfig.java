package com.cheatkey.common.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
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
        return new OpenAPI()
                .info(info)
//                .addServersItem(new Server().url("https://cheatkey.kr").description("운영 환경 (도메인)"))
                .addServersItem(new Server().url("https://mockzone.dev").description("운영 환경 (IP 접근)"));
    }
}
