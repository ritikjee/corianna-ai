package com.corianna.app_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;

import com.corianna.app_service.enums.RoleEnum;
import com.corianna.app_service.utils.GenerateKeys;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
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
public class Member implements java.io.Serializable {

    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    private String id;

    @Column(name = "app_role", nullable = false, unique = false)
    private RoleEnum role;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "app_id", nullable = false)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private App app;

    @PrePersist
    public void prePersist() {
        this.id = GenerateKeys.generateKey("member");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreRemove
    public void preRemove() {
        if (this.role == RoleEnum.OWNER) {
            throw new IllegalStateException("Cannot delete a member with role OWNER.");
        }
    }

}
