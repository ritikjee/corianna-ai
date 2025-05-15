package com.corianna.app_service.record;

public record KeyValue(
        String key,
        String value) {

    public KeyValue {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value cannot be null or blank");
        }
    }

}
