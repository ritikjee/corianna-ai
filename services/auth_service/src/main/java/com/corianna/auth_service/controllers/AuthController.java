package com.corianna.auth_service.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.auth_service.constants.CookieNames;
import com.corianna.auth_service.dto.ResponseDTO;
import com.corianna.auth_service.entity.Device;
import com.corianna.auth_service.entity.User;
import com.corianna.auth_service.record.AuthInput;
import com.corianna.auth_service.record.StateOutput;
import com.corianna.auth_service.services.AuthService;
import com.corianna.auth_service.utils.JwtConfig;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${jwt.secret.access-token}")
    private String accessTokenSecret;

    @Value("${jwt.secret.refresh-token}")
    private String refreshTokenSecret;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService, JwtConfig jwtConfig) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthInput input, HttpServletResponse response,
            HttpServletRequest request) {

        String userAgent = request.getHeader("User-Agent");

        try {
            Device device = authService.login(input.email(), input.password(), request.getRemoteAddr(), userAgent);
            Map<String, Object> userData = new HashMap<>();
            userData.put("sub", device.getUser().getId());
            userData.put("email", device.getUser().getEmail());
            userData.put("sid", device.getSessionId());

            if (device.getUser().getFirstname() != null) {
                userData.put("firstName", device.getUser().getFirstname());
            }
            if (device.getUser().getLastname() != null) {
                userData.put("lastName", device.getUser().getLastname());
            }
            if (device.getUser().getImage() != null) {
                userData.put("image", device.getUser().getImage());
            }

            String refreshToken = jwtConfig.encodeToken(refreshTokenSecret, 30L * 24 * 60 * 60 * 1000, userData);

            Cookie cookie = new Cookie(CookieNames.REFRESH_TOKEN_NAME, refreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(30 * 24 * 60 * 60);

            response.addCookie(cookie);

            return ResponseEntity.ok().body(
                    new ResponseDTO<>("Login Successful", 200, null, null, request.getRequestURI()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>("Email or password is incorrect 1", 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthInput input, HttpServletRequest request) {

        User user = new User();
        user.setEmail(input.email());
        user.setFirstname(input.firstName());
        user.setLastname(input.lastName());
        user.setPassword(input.password());

        try {
            String state = authService.register(user);

            StateOutput stateOutput = new StateOutput(state);

            return ResponseEntity.ok().body(
                    new ResponseDTO<>("Registration Successful", 201, null, stateOutput, request.getRequestURI()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>("Email already exists", 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam("state") String state, @RequestParam("otp") String otp,
            HttpServletRequest request) {
        try {
            String result = authService.verifyOtp(state, otp);
            return ResponseEntity.ok().body(
                    new ResponseDTO<>(result, 200, null, null, request.getRequestURI()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam("state") String state, HttpServletRequest request) {
        try {
            String result = authService.resendOTP(state);
            return ResponseEntity.ok().body(
                    new ResponseDTO<>(result, 200, null, null, request.getRequestURI()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String email, HttpServletRequest request) {
        try {
            String state = authService.forgotPassword(email);
            StateOutput stateOutput = new StateOutput(state);
            return ResponseEntity.ok().body(
                    new ResponseDTO<>("OTP sent for password reset", 200, null, stateOutput, request.getRequestURI()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String result = authService.resetPassword(body.get("state"), body.get("password"));
            return ResponseEntity.ok().body(
                    new ResponseDTO<>(result, 200, null, null, request.getRequestURI()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(e.getMessage(), 400, null, null, request.getRequestURI()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal Server Error", 500, null, null, request.getRequestURI()));
        }
    }

}
