package com.assignment.api_gateway.service;

import com.assignment.api_gateway.dto.CreateCommentRequest;
import com.assignment.api_gateway.dto.CreatePostRequest;
import com.assignment.api_gateway.dto.LikeRequest;
import com.assignment.api_gateway.exception.RateLimitExceededException;
import com.assignment.api_gateway.model.AuthorType;
import com.assignment.api_gateway.model.Comment;
import com.assignment.api_gateway.model.Post;
import com.assignment.api_gateway.repository.CommentRepository;
import com.assignment.api_gateway.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisGuardrailService guardrailService;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       RedisGuardrailService guardrailService,
                       NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.guardrailService = guardrailService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Post createPost(CreatePostRequest request) {
        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        int depthLevel = 1;
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            depthLevel = parentComment.getDepthLevel() + 1;
        }

        // Vertical Cap
        if (depthLevel > 20) {
            throw new RateLimitExceededException("Vertical Cap Exceeded: Comment thread cannot go deeper than 20 levels.");
        }

        if (request.getAuthorType() == AuthorType.BOT) {
            // Horizontal Cap
            guardrailService.checkHorizontalCap(postId);

            // Cooldown Cap & Notification
            // Assuming post author is the human for this interaction rule
            if (post.getAuthorType() == AuthorType.USER) {
                guardrailService.checkCooldownCap(request.getAuthorId(), post.getAuthorId());
                notificationService.handleBotInteraction(request.getAuthorId(), post.getAuthorId());
            }
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .depthLevel(depthLevel)
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);

        // Virality Score
        guardrailService.updateViralityScore(postId, request.getAuthorType(), false);

        return comment;
    }

    @Transactional
    public void likePost(Long postId, LikeRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Virality Score (human like)
        guardrailService.updateViralityScore(postId, AuthorType.USER, true);
    }
}
