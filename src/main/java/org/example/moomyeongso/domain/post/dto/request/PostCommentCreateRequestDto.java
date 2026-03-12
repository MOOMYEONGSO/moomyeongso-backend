package org.example.moomyeongso.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PostCommentCreateRequestDto(
        @NotBlank
        String content
) {
}
