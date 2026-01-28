package org.example.namelesschamber.domain.post.repository;

import org.example.namelesschamber.domain.post.entity.Post;
import org.example.namelesschamber.domain.post.entity.PostStatus;
import org.example.namelesschamber.domain.post.entity.PostType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String> {
    long countByUserId(String userId);
    List<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Post> findAllByTypeAndStatusOrderByCreatedAtDesc(PostType type, PostStatus status);
    List<Post> findAllByUserIdAndStatusOrderByCreatedAtDesc(String userId, PostStatus status);
    long countByTypeAndStatusAndCreatedAtBetween(PostType type, PostStatus status, Instant start, Instant end);
    long countByTypeAndStatus(PostType type, PostStatus status);
    Optional<Post> findByIdAndStatus(String id, PostStatus status);
}
