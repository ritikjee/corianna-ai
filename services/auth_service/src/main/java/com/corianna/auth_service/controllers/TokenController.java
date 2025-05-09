package com.corianna.auth_service.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.auth_service.constants.CookieNames;
import com.corianna.auth_service.dto.ResponseDTO;
import com.corianna.auth_service.utils.JwtConfig;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    @Value("${jwt.secret.access-token}")
    private String accessTokenSecret;

    @Value("${jwt.secret.refresh-token}")
    private String refreshTokenSecret;

    private final JwtConfig jwtConfig;

    public TokenController(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @GetMapping("/refresh")
    public ResponseEntity<ResponseDTO<?>> generateAccessToken(HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = (String) request.getAttribute("refresh-token");

        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(new ResponseDTO<>("Refresh token not found", 400, null, null, request.getRequestURI()));
        }

        try {

            Map<String, Object> claims = jwtConfig.decodeToken(refreshToken, refreshTokenSecret);

            if (claims == null) {
                return ResponseEntity.badRequest()
                        .body(new ResponseDTO<>("Invalid refresh token", 400, null, null, request.getRequestURI()));
            }

            String accessToken = jwtConfig.encodeToken(accessTokenSecret, 15 * 60 * 1000, claims);

            Cookie cookie = new Cookie(CookieNames.ACCESS_TOKEN_NAME, accessToken);

            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setSecure(true);
            cookie.setMaxAge(15 * 60);

            response.addCookie(cookie);

            return ResponseEntity.ok()
                    .body(new ResponseDTO<>("Access token generated successfully", 200, null, null,
                            request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ResponseDTO<>("Internal server error", 500, "Unauthorised", null,
                            request.getRequestURI()));
        }

    }

}
