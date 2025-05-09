package com.corianna.auth_service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.corianna.auth_service.utils.GenerateKeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "devices")
public class Device implements Serializable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "device_type", nullable = false)
    private String deviceType;

    @Column(name = "os_type", nullable = false)
    private String os;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "device_agent")
    private String deviceAgent;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    public void prePersist() {
        this.id = GenerateKeys.generateKey("dev");
        this.loginTime = LocalDateTime.now();
        this.sessionId = GenerateKeys.generateKey("sess");
    }
}