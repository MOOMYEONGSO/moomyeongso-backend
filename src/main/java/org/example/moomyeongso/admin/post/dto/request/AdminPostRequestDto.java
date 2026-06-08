package org.example.moomyeongso.admin.post.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPostRequestDto(
        @NotNull
        String content
) {
}
