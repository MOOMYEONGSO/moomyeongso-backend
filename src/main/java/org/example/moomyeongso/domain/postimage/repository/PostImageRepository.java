package org.example.moomyeongso.domain.postimage.repository;

import org.example.moomyeongso.domain.postimage.entity.PostImage;
import org.example.moomyeongso.domain.postimage.entity.PostImageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface PostImageRepository extends MongoRepository<PostImage, String> {

    List<PostImage> findAllByIdInAndUserId(List<String> ids, String userId);

    List<PostImage> findAllByPostIdAndStatus(String postId, PostImageStatus status);

    List<PostImage> findAllByStatusAndDeleteAfterLessThanEqual(PostImageStatus status, Instant now, Pageable pageable);

    List<PostImage> findAllByStatusAndCreatedAtBefore(PostImageStatus status, Instant cutoff, Pageable pageable);
}

