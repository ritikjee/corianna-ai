package com.corianna.integration_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.corianna.integration_service.enums.RoleEnum;

public record Member(
        String id,
        RoleEnum role,
        LocalDateTime updatedAt,
        LocalDateTime createdAt,
        String userId,
        App app) implements Serializable {

}
