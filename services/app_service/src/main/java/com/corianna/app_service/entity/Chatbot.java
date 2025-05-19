package com.corianna.app_service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;

import com.corianna.app_service.utils.GenerateKeys;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chatbot implements Serializable {

    @Id
    @Column(name = "chatbot_id", nullable = false, unique = true)
    private String id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "app_id", nullable = false)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private App app;

    @Column(name = "api_key", nullable = false, unique = true, updatable = false)
    private String apiKey;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = GenerateKeys.generateKey("chatbot");
        this.apiKey = GenerateKeys.generateKey("api");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
