package com.corianna.app_service.utils;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateKeys {

    public static String generateKey(String prefix) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        return String.format("%s_%s", prefix, token);
    }

}