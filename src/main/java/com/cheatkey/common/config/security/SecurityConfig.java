package com.cheatkey.common.config.security;

import com.cheatkey.common.config.swagger.SwaggerMockOAuthFilter;
import com.cheatkey.common.handler.oauth.KakaoAuthenticationSuccessHandler;
import com.cheatkey.common.sercurity.oauth.KakaoOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final KakaoOAuth2UserService oAuth2UserService;
    private final KakaoAuthenticationSuccessHandler successHandler;
    private final SwaggerMockOAuthFilter swaggerMockOAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/auth/login")
                        .userInfoEndpoint(user -> user.userService(oAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(new SimpleUrlAuthenticationFailureHandler("/auth/login?error=true"))
                )
                .addFilterBefore(swaggerMockOAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}


