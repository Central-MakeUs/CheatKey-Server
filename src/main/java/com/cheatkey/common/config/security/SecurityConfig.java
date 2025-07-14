package com.cheatkey.common.config.security;

import com.cheatkey.common.config.security.matcher.SkipPathRequestMatcher;
import com.cheatkey.module.auth.interfaces.handler.OAuthSuccessHandler;
import com.cheatkey.common.jwt.JwtUtil;
import com.cheatkey.module.auth.provider.NativeAppAuthProvider;
import com.cheatkey.common.config.security.filter.JwtAuthenticationFilter;
import com.cheatkey.common.config.security.filter.JwtExceptionFilter;
import com.cheatkey.module.auth.filter.NativeAppLoginFilter;

import com.cheatkey.module.auth.interfaces.oauth.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final OAuthSuccessHandler oAuthSuccessHandler;
    private final NativeAppAuthProvider nativeAppAuthProvider;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;

    private static final String LOGIN_URL = "/v1/api/auth/login";
    private static final String API_ROOT_URL = "/v1/api/**";
    private static final String[] WHITE_LIST = {
            "/v1/api/auth/login",
            "/v1/api/auth/register/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String[] JWT_SKIP_URL = {
            "/v1/api/auth/login",
            "/v1/api/auth/register/**",
    };

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(nativeAppAuthProvider)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .addFilterAt(nativeAppLoginFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter(), NativeAppLoginFilter.class)
                .addFilterBefore(new JwtExceptionFilter(), JwtAuthenticationFilter.class)

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuthSuccessHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    private NativeAppLoginFilter nativeAppLoginFilter(AuthenticationManager authenticationManager) {
        NativeAppLoginFilter filter = new NativeAppLoginFilter(authenticationManager, jwtUtil);
        filter.setFilterProcessesUrl(LOGIN_URL);
        return filter;
    }

    private JwtAuthenticationFilter jwtAuthenticationFilter() {
        var matcher = new SkipPathRequestMatcher(List.of(JWT_SKIP_URL), API_ROOT_URL);
        return new JwtAuthenticationFilter(matcher, jwtUtil);
    }
}
