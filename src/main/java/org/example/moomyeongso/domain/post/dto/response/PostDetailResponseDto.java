package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record PostDetailResponseDto(
        String postId,
        PostType type,
        String from,
        String to,
        String content,
        long likes,
        long commentCount,
        long views,
        Instant createdAt,
        int coin,
        List<PostImageResponseDto> images,
        List<PostCommentResponseDto> comments
) {
    public static PostDetailResponseDto from(Post post, int coin, List<PostCommentResponseDto> comments) {
        long commentCount = comments.size();
        List<PostImageResponseDto> images = Optional.ofNullable(post.getImages()).orElse(List.of()).stream()
                .map(PostImageResponseDto::from)
                .toList();
        return new PostDetailResponseDto(
                post.getId(),
                post.getType(),
                post.getFrom(),
                post.getTo(),
                post.getContent(),
                post.getLikes(),
                commentCount,
                post.getViews(),
                post.getCreatedAt(),
                coin,
                images,
                comments
        );
    }
}
