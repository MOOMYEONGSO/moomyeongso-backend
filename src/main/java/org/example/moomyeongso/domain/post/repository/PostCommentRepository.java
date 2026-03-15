package org.example.moomyeongso.domain.post.repository;

import org.example.moomyeongso.domain.post.entity.PostComment;
import org.example.moomyeongso.domain.post.entity.PostCommentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PostCommentRepository extends MongoRepository<PostComment, String> {
    List<PostComment> findAllByPostIdAndStatusOrderByCreatedAtAsc(String postId, PostCommentStatus status);
    Optional<PostComment> findByIdAndPostIdAndStatus(String id, String postId, PostCommentStatus status);
}
