package org.example.namelesschamber.admin.post.dto.response;

import org.example.namelesschamber.domain.post.entity.Post;
import org.example.namelesschamber.domain.post.entity.PostType;

import java.time.Instant;

public record AdminPostResponseDto(
        String postId,
        String title,
        String content,
        String userId,
        PostType type,
        boolean isDeleted,
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
                post.isDeleted(),
                post.getViews(),
                post.getLikes(),
                post.getCreatedAt()
        );
    }
}