package com.cheatkey.common.config.security.filter;

import java.io.IOException;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        } catch (ExpiredJwtException e) {
            setErrorResponse(response, new CustomException(ErrorCode.TOKEN_EXPIRED));
        } catch (MalformedJwtException | SignatureException e) {
            setErrorResponse(response, new CustomException(ErrorCode.MALFORMED_TOKEN));
        } catch (Exception e) {
            setErrorResponse(response, new CustomException(ErrorCode.INVALID_TOKEN));
        }
    }

    private void setErrorResponse(HttpServletResponse response, CustomException e)
            throws IOException {
        log.error("JWT 인증 에러 발생: {}", e.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                e.getErrorCode().name(),
                e.getMessage()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
