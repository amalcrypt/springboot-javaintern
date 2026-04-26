package com.assignment.api_gateway.dto;

import com.assignment.api_gateway.model.AuthorType;
import lombok.Data;

@Data
public class CreatePostRequest {
    private Long authorId;
    private AuthorType authorType;
    private String content;
}
