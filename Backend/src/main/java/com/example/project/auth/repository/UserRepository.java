package com.example.project.auth.repository;

import java.util.UUID;

import com.example.project.auth.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
