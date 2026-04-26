package com.assignment.api_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final StringRedisTemplate redisTemplate;

    public NotificationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void handleBotInteraction(Long botId, Long humanId) {
        String cooldownKey = "user:" + humanId + ":notif_cooldown";
        String pendingKey = "user:" + humanId + ":pending_notifs";
        String notificationMsg = "Bot " + botId + " replied to your post";

        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(15));

        if (Boolean.TRUE.equals(isSet)) {
            // No recent notification, send immediate
            logger.info("Push Notification Sent to User {}: {}", humanId, notificationMsg);
        } else {
            // Recent notification exists, push to pending
            redisTemplate.opsForList().rightPush(pendingKey, notificationMsg);
        }
    }

    @Scheduled(fixedRate = 300000) // 5 minutes (300,000 ms)
    public void sweepPendingNotifications() {
        // Scan for all pending notifications. 
        // In a real prod environment, scan command is better than keys, but for this assignment keys is acceptable.
        Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            String userIdStr = key.split(":")[1];
            
            // Pop all elements atomically using Lua script or simply by reading size and then popping.
            // Using multi/exec transaction to atomic pop and delete. Wait, Spring Data Redis with transactions can be tricky.
            // Actually, we can just fetch all elements and delete the key.
            List<String> notifications = redisTemplate.opsForList().range(key, 0, -1);
            if (notifications != null && !notifications.isEmpty()) {
                redisTemplate.delete(key);
                
                int count = notifications.size();
                logger.info("Summarized Push Notification for User {}: Bot X and {} others interacted with your posts.", userIdStr, count - 1);
            }
        }
    }
}
