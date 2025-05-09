package com.corianna.auth_service.filter;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.corianna.auth_service.constants.CookieNames;
import com.corianna.auth_service.dto.ResponseDTO;
import com.corianna.auth_service.utils.JwtConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret.access-token}")
    private String accessTokenSecret;

    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthenticationFilter(JwtConfig jwtConfig, RedisTemplate<String, String> redisTemplate) {
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {

            String URI = request.getRequestURI();

            if (URI.startsWith("/api/auth")) {
                filterChain.doFilter(request, response);
                return;
            }

            Cookie[] cookies = request.getCookies();

            if (cookies == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is missing", URI);
                return;
            }

            String token = null;

            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(CookieNames.ACCESS_TOKEN_NAME)) {
                    token = cookie.getValue();
                    break;
                }
            }

            if (token == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is missing", URI);
                return;
            }

            Map<String, Object> claims = jwtConfig.decodeToken(token, accessTokenSecret);

            if (claims == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid", URI);
                return;
            }

            String sessionId = (String) claims.get("sid");

            if (sessionId == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid", URI);
                return;
            }

            String isSessionDeleted = redisTemplate.opsForValue().get("exp_sess::" + sessionId);

            if (isSessionDeleted != null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Session Expired", URI);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid",
                    request.getRequestURI());
        }

    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message, String path)
            throws IOException {
        ResponseDTO<?> responseDTO = new ResponseDTO<>(
                message,
                status,
                "Unauthorized",
                null,
                path);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ObjectMapper objectMapper = new ObjectMapper();

        response.getWriter().write(objectMapper.writeValueAsString(responseDTO));
    }
}
