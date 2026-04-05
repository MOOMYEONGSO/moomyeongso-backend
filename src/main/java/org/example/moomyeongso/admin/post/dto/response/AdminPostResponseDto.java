package org.example.moomyeongso.admin.post.dto.response;

import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;

import java.time.Instant;

public record AdminPostResponseDto(
        String postId,
        String title,
        String content,
        String userId,
        PostType type,
        PostStatus status,
        long commentCount,
        long views,
        long likes,
        Instant createdAt
) {
    public static AdminPostResponseDto from(Post post) {
        return new AdminPostResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUserId(),
                post.getType(),
                post.getStatus(),
                post.getCommentCount(),
                post.getViews(),
                post.getLikes(),
                post.getCreatedAt()
        );
    }
}
