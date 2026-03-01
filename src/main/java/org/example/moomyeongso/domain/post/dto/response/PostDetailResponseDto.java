package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.Post;

import java.time.Instant;
import java.util.List;

public record PostDetailResponseDto(
        String postId,
        String title,
        String content,
        long likes,
        long views,
        Instant createdAt,
        int coin,
        List<PostCommentResponseDto> comments
) {
    public static PostDetailResponseDto from(Post post, int coin, List<PostCommentResponseDto> comments) {
        return new PostDetailResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getLikes(),
                post.getViews(),
                post.getCreatedAt(),
                coin,
                comments
        );
    }
}
