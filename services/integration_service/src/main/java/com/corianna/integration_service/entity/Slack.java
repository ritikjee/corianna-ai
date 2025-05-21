package com.corianna.integration_service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.corianna.integration_service.utils.GenerateKeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slack implements Serializable {

    @Id
    private String id;

    @Column(nullable = false)
    private String appId;

    @Column(nullable = false)
    private String slackAppId;

    @Column(nullable = false)
    private String slackUserId;

    @Column(name = "user_token", nullable = false, length = 2048)
    private String userToken;

    @Column(name = "access_token", nullable = false, length = 2048)
    private String accessToken;

    @Column(name = "bot_user_id", nullable = false)
    private String botUserId;

    @Column(name = "team_id", nullable = false)
    private String teamId;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.id = GenerateKeys.generateKey("slack");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
