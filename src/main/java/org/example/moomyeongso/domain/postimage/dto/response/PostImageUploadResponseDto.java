package org.example.moomyeongso.domain.postimage.dto.response;

import java.util.List;

public record PostImageUploadResponseDto(
        List<UploadedPostImageResponseDto> images
) {
    public static PostImageUploadResponseDto of(List<UploadedPostImageResponseDto> images) {
        return new PostImageUploadResponseDto(images);
    }
}

