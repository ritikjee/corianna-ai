package com.corianna.app_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDTO<T> {

    private String message;
    private int status;
    private String error;
    private T data;
    private String path;
    private long timestamp;

    public ResponseDTO(String message, int status, String error, T data, String path) {
        this.message = message;
        this.status = status;
        this.error = error;
        this.data = data;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }

}