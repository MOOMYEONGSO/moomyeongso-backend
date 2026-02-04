package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.Post;

import java.time.Instant;
import java.util.List;

public record PostPreviewResponseDto(
        String postId,
        String userId,
        String title,
        String contentPreview,
        int contentLength,
        List<String> tags,
        long likes,
        long views,
        Instant createdAt
) {
    private static final int PREVIEW_MAX_LENGTH = 100;
    private static final String ELLIPSIS = "...";

    public static PostPreviewResponseDto from(Post post) {
        String content = post.getContent() == null ? "" : post.getContent();

        String preview = content.length() > PREVIEW_MAX_LENGTH
                ? content.substring(0, PREVIEW_MAX_LENGTH) + ELLIPSIS
                : content;

        return new PostPreviewResponseDto(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                preview,
                content.length(),
                post.getTags(),
                post.getLikes(),
                post.getViews(),
                post.getCreatedAt()
        );
    }
}
