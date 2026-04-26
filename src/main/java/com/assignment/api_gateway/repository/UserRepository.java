package com.assignment.api_gateway.repository;

import com.assignment.api_gateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
