package com.assignment.api_gateway.service;

import com.assignment.api_gateway.exception.RateLimitExceededException;
import com.assignment.api_gateway.model.AuthorType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisGuardrailService {

    private final StringRedisTemplate redisTemplate;

    public RedisGuardrailService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateViralityScore(Long postId, AuthorType authorType, boolean isLike) {
        String key = "post:" + postId + ":virality_score";
        long incrementValue = 0;

        if (authorType == AuthorType.BOT) {
            incrementValue = 1;
        } else if (authorType == AuthorType.USER) {
            if (isLike) {
                incrementValue = 20;
            } else {
                incrementValue = 50; // comment
            }
        }

        if (incrementValue > 0) {
            redisTemplate.opsForValue().increment(key, incrementValue);
        }
    }

    public void checkHorizontalCap(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount != null && currentCount > 100) {
            // decrement back so it stays at 100 conceptually, though it doesn't strictly matter
            redisTemplate.opsForValue().decrement(key);
            throw new RateLimitExceededException("Horizontal Cap Exceeded: A post cannot have more than 100 bot replies.");
        }
    }

    public void checkCooldownCap(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        
        if (Boolean.FALSE.equals(isSet)) {
            throw new RateLimitExceededException("Cooldown Cap Exceeded: Bot cannot interact with the same Human more than once per 10 minutes.");
        }
    }
}
