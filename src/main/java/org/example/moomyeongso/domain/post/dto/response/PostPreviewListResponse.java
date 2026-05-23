package org.example.moomyeongso.domain.post.dto.response;

import java.util.List;

public record PostPreviewListResponse(
        int coin,
        List<PostPreviewResponseDto> posts
) {
    public static PostPreviewListResponse of(List<PostPreviewResponseDto> posts, int coin) {
        return new PostPreviewListResponse(coin, posts);
    }
}
