package com.corianna.app_service.interceptor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.corianna.app_service.dto.ResponseDTO;
import com.corianna.app_service.dto.UserDTO;
import com.corianna.app_service.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${secrets.x-app-secret}")
    private String xAppSecret;

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String URI = request.getRequestURI();

        if (URI.startsWith("/api/ingest")) {
            return true;
        }

        if (URI.startsWith("/api/internal-services")) {
            String xAppSecretHeader = request.getHeader("x-app-secret");
            if (xAppSecretHeader == null || !xAppSecretHeader.equals(xAppSecret)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "🚫 Access Denied! Please log in to continue. 🌟", request.getRequestURI());
                return false;
            }
            return true;
        }

        try {
            String userId = request.getHeader("userId");

            if (userId != null && !userId.isEmpty()) {
                request.setAttribute("userId", userId);
                return true;
            }

            String __session = null;

            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("__session")) {
                        __session = cookie.getValue();
                        break;
                    }
                }
            }

            if (__session == null || __session.isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "🚫 Access Denied! Please log in to continue. 🌟", request.getRequestURI());
                return false;
            }

            UserDTO user = authService.getUser();

            if (user != null) {
                request.setAttribute("userId", user.getId());
                return true;
            }

            sendErrorResponse(response, 401, "🚫 Access Denied! Please log in to continue. 🌟",
                    request.getRequestURI());
            return false;
        } catch (Exception e) {
            return false;
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
