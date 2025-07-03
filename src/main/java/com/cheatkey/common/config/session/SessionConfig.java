package com.cheatkey.common.config.session;

import jakarta.servlet.SessionCookieConfig;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> sessionCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            SessionCookieConfig sessionCookieConfig = context.getServletContext().getSessionCookieConfig();
            sessionCookieConfig.setMaxAge(60 * 60 * 24 * 14); // 14일 (초 단위)
        });
    }
}
