package com.assignment.api_gateway.controller;

import com.assignment.api_gateway.dto.CreateCommentRequest;
import com.assignment.api_gateway.dto.CreatePostRequest;
import com.assignment.api_gateway.dto.LikeRequest;
import com.assignment.api_gateway.model.Comment;
import com.assignment.api_gateway.model.Post;
import com.assignment.api_gateway.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody CreatePostRequest request) {
        Post post = postService.createPost(request);
        return new ResponseEntity<>(post, HttpStatus.CREATED);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long postId,
                                              @RequestBody CreateCommentRequest request) {
        Comment comment = postService.addComment(postId, request);
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId,
                                         @RequestBody LikeRequest request) {
        postService.likePost(postId, request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
