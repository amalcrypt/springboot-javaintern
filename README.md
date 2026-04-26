# Spring Boot API Gateway & Guardrail Service

This repository contains a Spring Boot microservice acting as a central API gateway and guardrail system. It demonstrates the ability to handle concurrent requests, manage distributed state using Redis, and implement event-driven scheduling.

## Tech Stack
- Java 17
- Spring Boot 3.2.x
- PostgreSQL (JPA/Hibernate)
- Redis (Spring Data Redis)
- Docker & Docker Compose

## Features
- **Core API**: Create posts, add comments, and like posts.
- **Virality Engine**: Instantly update post virality scores using Redis based on bot replies (+1), human likes (+20), and human comments (+50).
- **Atomic Locks (Guardrails)**:
  - **Horizontal Cap**: Limits a single post to a maximum of 100 bot replies to prevent runaway spam. Enforced using atomic Redis increment operations. Returns HTTP 429 when exceeded.
  - **Vertical Cap**: Rejects comment threads exceeding a depth level of 20.
  - **Cooldown Cap**: Limits bots from interacting with the same human more than once every 10 minutes.
- **Notification Engine**:
  - **Redis Throttler**: Only allows push notifications once every 15 minutes per user. Subsequent interactions are queued in Redis as pending notifications.
  - **CRON Sweeper**: A scheduled task running every 5 minutes summarizes and pops pending notifications from Redis, printing them to the console.

## Architecture & Concurrency
- **Statelessness**: The Spring Boot app is completely stateless. Counters, cooldowns, and notification states are stored entirely in Redis.
- **Data Integrity**: PostgreSQL is the single source of truth for application content, but no DB transactions begin if Redis guardrails block the request. This prevents DB bloat during high-velocity spam attacks.
- **Race Condition Prevention & Thread Safety**: 
  To guarantee thread safety for the Atomic Locks in Phase 2, the application exclusively relies on Redis atomic operations rather than Java-level synchronized blocks or database-level row locking.
  - **Horizontal Cap**: We use `redisTemplate.opsForValue().increment(key)`. Redis processes commands sequentially in a single thread. `INCR` is an atomic operation, meaning even if 200 bots execute it at the exact same millisecond, Redis will accurately count from 1 to 200 without any race conditions or lost updates. The Java application simply checks if the atomically returned incremented value exceeds 100 and rejects the request immediately without hitting the database.
  - **Cooldown Cap**: We use `redisTemplate.opsForValue().setIfAbsent(key, value, duration)`. This directly translates to the Redis `SETNX` (Set if Not eXists) command. It atomically checks if the key exists and sets it in one operation. If multiple bots try to interact simultaneously, only the first one will receive `true` from Redis, while the others receive `false` and are blocked.
  - **Virality Score**: Implemented using atomic `INCRBY` so concurrent likes and replies always yield the perfectly computed sum without any read-modify-write race conditions.

## Running the Application
1. Start the infrastructure (PostgreSQL and Redis) using Docker:
   ```bash
   docker-compose up -d
   ```
2. Build and run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Testing
- Import the included `postman_collection.json` into Postman to test the endpoints.
- To simulate the Race Conditions test, you can use a load testing tool like `hey` or `apache benchmark (ab)` against the `POST /api/posts/{postId}/comments` endpoint.

## Endpoints Summary
- `POST /api/posts`: Payload `{ authorId, authorType, content }`
- `POST /api/posts/{postId}/comments`: Payload `{ authorId, authorType, content, parentCommentId }`
- `POST /api/posts/{postId}/like`: Payload `{ userId }`
