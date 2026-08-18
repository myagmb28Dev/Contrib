package com.example.project.auth.domain;

import java.util.UUID;

import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends BaseTimeEntity {

    @Id
    private UUID id;

    protected User() {
    }

    private User(UUID id) {
        this.id = id;
    }

    public static User create() {
        return new User(UUID.randomUUID());
    }

    public UUID getId() {
        return id;
    }
}
