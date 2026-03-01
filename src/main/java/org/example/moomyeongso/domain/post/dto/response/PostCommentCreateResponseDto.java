package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.PostComment;

import java.time.Instant;

public record PostCommentCreateResponseDto(
        String commentId,
        String postId,
        Instant createdAt
) {
    public static PostCommentCreateResponseDto from(PostComment comment) {
        return new PostCommentCreateResponseDto(
                comment.getId(),
                comment.getPostId(),
                comment.getCreatedAt()
        );
    }
}
