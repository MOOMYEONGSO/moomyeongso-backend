package org.example.moomyeongso.domain.post.repository;

import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String> {
    long countByUserId(String userId);
    List<Post> findAllByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Post> findAllByStatusAndUserIdNotOrderByIdDesc(PostStatus status, String userId, Pageable pageable);
    List<Post> findAllByStatusAndUserIdNotAndIdLessThanOrderByIdDesc(PostStatus status, String userId, String cursor, Pageable pageable);
    List<Post> findAllByTypeAndStatusOrderByCreatedAtDesc(PostType type, PostStatus status);
    List<Post> findAllByTypeAndStatusAndUserIdNotOrderByIdDesc(PostType type, PostStatus status, String userId, Pageable pageable);
    List<Post> findAllByTypeAndStatusAndUserIdNotAndIdLessThanOrderByIdDesc(PostType type, PostStatus status, String userId, String cursor, Pageable pageable);
    List<Post> findAllByUserIdAndStatusOrderByCreatedAtDesc(String userId, PostStatus status);
    List<Post> findAllByUserIdAndTypeAndStatusOrderByCreatedAtDesc(String userId, PostType type, PostStatus status);
    long countByTypeAndStatusAndCreatedAtBetween(PostType type, PostStatus status, Instant start, Instant end);
    long countByTypeAndStatus(PostType type, PostStatus status);
    Optional<Post> findByIdAndStatus(String id, PostStatus status);
}
