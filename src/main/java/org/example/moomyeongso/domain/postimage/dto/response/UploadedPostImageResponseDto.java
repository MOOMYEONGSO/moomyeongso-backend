package org.example.moomyeongso.domain.postimage.dto.response;

import org.example.moomyeongso.domain.postimage.entity.PostImage;

public record UploadedPostImageResponseDto(
        String imageId,
        String imageUrl,
        String thumbnailUrl,
        String contentType,
        long size,
        int width,
        int height
) {
    public static UploadedPostImageResponseDto from(PostImage image) {
        return new UploadedPostImageResponseDto(
                image.getId(),
                image.getImageUrl(),
                image.getThumbnailUrl(),
                image.getContentType(),
                image.getSize(),
                image.getWidth(),
                image.getHeight()
        );
    }
}

