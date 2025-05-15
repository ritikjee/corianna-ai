package com.corianna.app_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;

import com.corianna.app_service.utils.GenerateKeys;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

@Entity
@Data
public class IngestionAPI {

    @Id
    @Column(name = "ingestion_api_id", nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String apiKey;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "app_id", nullable = false)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private App app;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = GenerateKeys.generateKey("ingestion_api");
        this.apiKey = GenerateKeys.generateKey("apikey");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
