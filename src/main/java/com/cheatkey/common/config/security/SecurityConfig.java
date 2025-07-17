package com.cheatkey.common.config.security;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.common.config.security.filter.JwtAuthenticationFilter;
import com.cheatkey.common.config.security.matcher.SkipPathRequestMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String LOGIN_URL = "/v1/api/auth/login";
    private static final String API_ROOT_URL = "/v1/api/**";
    private static final String[] WHITE_LIST = {
            LOGIN_URL,
            "/v1/api/files/upload",
            "/swagger-ui/**",
            "/v3/api-docs/**",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
                new SkipPathRequestMatcher(List.of(WHITE_LIST), "/v1/api/**"),
                jwtProvider
        );

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers("/v1/api/**").hasRole("USER")
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
