package com.corianna.integration_service.dto;

public record DataResponse<T>(
        int status,
        T data) {

}
