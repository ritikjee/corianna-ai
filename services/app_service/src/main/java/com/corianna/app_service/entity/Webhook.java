package com.corianna.app_service.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.OnDelete;

import com.corianna.app_service.utils.GenerateKeys;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
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
public class Webhook implements Serializable {

    @Id
    @Column(name = "webhook_id", nullable = false, unique = true)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String token;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "webhook_headers", joinColumns = @JoinColumn(name = "webhook_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private Map<String, String> headers;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "app_id", nullable = false)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private App app;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = GenerateKeys.generateKey("webhook");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
