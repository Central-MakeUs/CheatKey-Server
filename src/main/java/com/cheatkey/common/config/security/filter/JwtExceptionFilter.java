package com.cheatkey.common.config.security.filter;

import java.io.IOException;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ErrorResponse;
import com.cheatkey.common.exception.JwtAuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtExceptionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException e) {
            // JWT 인증 예외를 401로 처리
            setErrorResponse(response, new CustomException(ErrorCode.INVALID_TOKEN));
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, new CustomException(ErrorCode.TOKEN_EXPIRED));
        } catch (MalformedJwtException | SignatureException e) {
            setErrorResponse(response, new CustomException(ErrorCode.MALFORMED_TOKEN));
        } catch (Exception e) {
            // JWT 관련 예외가 아닌 경우는 다시 던져서 다른 핸들러가 처리하도록 함
            if (e instanceof ExpiredJwtException || e instanceof MalformedJwtException || e instanceof SignatureException) {
                setErrorResponse(response, new CustomException(ErrorCode.INVALID_TOKEN));
            } else {
                throw e;
            }
        }
    }

    private void setErrorResponse(HttpServletResponse response, CustomException e)
            throws IOException {
        log.error("JWT 인증 에러 발생: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                e.getErrorCode().name(),
                e.getMessage()
        );

        // JWT 관련 예외는 항상 401 Unauthorized로 처리
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
