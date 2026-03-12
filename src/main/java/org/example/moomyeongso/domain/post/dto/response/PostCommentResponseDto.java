package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.PostComment;

import java.time.Instant;

public record PostCommentResponseDto(
        String commentId,
        String authorId,
        String authorNickname,
        String content,
        Instant createdAt,
        boolean mine
) {
    public static PostCommentResponseDto from(PostComment comment, String currentUserId) {
        return new PostCommentResponseDto(
                comment.getId(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getAuthorId().equals(currentUserId)
        );
    }
}
