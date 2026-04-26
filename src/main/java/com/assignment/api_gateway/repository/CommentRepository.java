package com.assignment.api_gateway.repository;

import com.assignment.api_gateway.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
