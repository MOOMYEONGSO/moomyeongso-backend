package org.example.moomyeongso.domain.post.repository;

import org.example.moomyeongso.domain.post.entity.PostComment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PostCommentRepository extends MongoRepository<PostComment, String> {
    List<PostComment> findAllByPostIdOrderByCreatedAtAsc(String postId);
    Optional<PostComment> findByIdAndPostId(String id, String postId);
}
