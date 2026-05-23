package org.example.moomyeongso.domain.post.dto.response;

import java.util.List;

public record PostPreviewCursorListResponse(
        int coin,
        String nextCursor,
        List<PostPreviewResponseDto> posts
) {
    public static PostPreviewCursorListResponse of(List<PostPreviewResponseDto> posts, int coin, String nextCursor) {
        return new PostPreviewCursorListResponse(coin, nextCursor, posts);
    }
}
