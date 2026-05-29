package org.example.moomyeongso.domain.post.dto.response;

import org.example.moomyeongso.domain.post.entity.PostImageAttachment;

public record PostImageResponseDto(
        String imageId,
        String imageUrl,
        String thumbnailUrl,
        int sortOrder
) {
    public static PostImageResponseDto from(PostImageAttachment image) {
        return new PostImageResponseDto(
                image.getImageId(),
                image.getImageUrl(),
                image.getThumbnailUrl(),
                image.getSortOrder()
        );
    }
}

