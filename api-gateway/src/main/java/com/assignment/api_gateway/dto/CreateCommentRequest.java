package com.assignment.api_gateway.dto;

import com.assignment.api_gateway.model.AuthorType;
import lombok.Data;

@Data
public class CreateCommentRequest {
    private Long authorId;
    private AuthorType authorType;
    private String content;
    private Long parentCommentId; // optional
}
