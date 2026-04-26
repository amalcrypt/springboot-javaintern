package com.assignment.api_gateway.repository;

import com.assignment.api_gateway.model.Bot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotRepository extends JpaRepository<Bot, Long> {
}
