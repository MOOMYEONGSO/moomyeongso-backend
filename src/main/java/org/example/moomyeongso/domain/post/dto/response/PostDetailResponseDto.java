package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostType;

import java.time.Instant;
import java.util.List;

public record PostDetailResponseDto(
        String postId,
        PostType type,
        String title,
        String content,
        long likes,
        long commentCount,
        long views,
        Instant createdAt,
        int coin,
        List<PostCommentResponseDto> comments
) {
    public static PostDetailResponseDto from(Post post, int coin, List<PostCommentResponseDto> comments) {
        long commentCount = comments.size();
        return new PostDetailResponseDto(
                post.getId(),
                post.getType(),
                post.getTitle(),
                post.getContent(),
                post.getLikes(),
                commentCount,
                post.getViews(),
                post.getCreatedAt(),
                coin,
                comments
        );
    }
}
