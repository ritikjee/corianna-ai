package com.corianna.auth_service.record;

public record AuthInput(String email, String password, String firstName, String lastName) {

    public AuthInput {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

    }
}