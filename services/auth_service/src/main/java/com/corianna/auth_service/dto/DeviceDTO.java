package com.corianna.auth_service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDTO implements Serializable {

    private String id;
    private String ipAddress;
    private String deviceType;
    private String os;
    private LocalDateTime loginTime;
    private String deviceAgent;

}
