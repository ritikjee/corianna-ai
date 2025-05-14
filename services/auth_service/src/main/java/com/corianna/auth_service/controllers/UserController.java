package com.corianna.auth_service.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corianna.auth_service.constants.CookieNames;
import com.corianna.auth_service.dto.ResponseDTO;
import com.corianna.auth_service.dto.UserDTO;
import com.corianna.auth_service.entity.User;
import com.corianna.auth_service.services.DeviceService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/user")
public class UserController {

    // TODO: Remove particular session
    // TODO: change password, update user details

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final DeviceService deviceService;

    public UserController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(HttpServletRequest request) {

        try {
            Object userObj = request.getAttribute("user");
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (userObj instanceof Map) ? (Map<String, Object>) userObj : null;

            if (user == null) {
                return ResponseEntity
                        .badRequest()
                        .body(new ResponseDTO<>("User not found", 400, null, null, request.getRequestURI()));
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setId((String) user.get("sub"));
            userDTO.setEmail((String) user.get("email"));
            userDTO.setFirstname((String) user.get("firstName"));
            userDTO.setLastname((String) user.get("lastName"));
            userDTO.setImage((String) user.get("image"));

            return ResponseEntity.ok(new ResponseDTO<>("User Info", 200, null, userDTO, request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal server error", 500, e.getMessage(), null, request.getRequestURI()));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = (String) request.getHeader("sessionId");

        try {
            deviceService.removeDevice(sessionId);

            Cookie cookie = new Cookie("token", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);

            response.addCookie(cookie);

            return ResponseEntity
                    .ok(new ResponseDTO<>("Logged out successfully", 200, null, null, request.getRequestURI()));

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal server error", 500, e.getMessage(), null, request.getRequestURI()));
        }
    }

    @GetMapping("/logout-all")
    public ResponseEntity<?> logoutAll(HttpServletRequest request) {

        User user = (User) request.getAttribute("user");
        String sessionId = (String) request.getAttribute("sessionId");

        Cookie cookie = new Cookie(CookieNames.ACCESS_TOKEN_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        Cookie cookie2 = new Cookie(CookieNames.REFRESH_TOKEN_NAME, null);
        cookie2.setHttpOnly(true);
        cookie2.setSecure(true);
        cookie2.setPath("/");
        cookie2.setMaxAge(0);

        try {
            deviceService.logoutAllDevices(user.getEmail(), sessionId);

            return ResponseEntity
                    .ok(new ResponseDTO<>("Logged out successfully", 201, null, null, request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal server error", 500, e.getMessage(), null, request.getRequestURI()));
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<?> getDevices(HttpServletRequest request) {

        User user = (User) request.getAttribute("user");

        try {
            return ResponseEntity.ok(new ResponseDTO<>("Device Info", 200, null,
                    deviceService.getAllDevices(user.getEmail()), request.getRequestURI()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ResponseDTO<>("Internal server error", 500, e.getMessage(), null, request.getRequestURI()));
        }
    }

}