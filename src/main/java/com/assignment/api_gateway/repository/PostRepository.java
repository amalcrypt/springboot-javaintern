package com.assignment.api_gateway.repository;

import com.assignment.api_gateway.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
