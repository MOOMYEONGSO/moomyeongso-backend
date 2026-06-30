package org.example.moomyeongso.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.moomyeongso.domain.post.entity.Post;

public record PostCreateRequestDto(
        @Size(max = 30)
        String from,
        @Size(max = 30)
        String to,
        @NotBlank
        String content
) {
    public PostCreateRequestDto {
        from = normalizeDisplayName(from);
        to = normalizeDisplayName(to);
    }

    private static String normalizeDisplayName(String value) {
        return value == null || value.isBlank() ? Post.DEFAULT_DISPLAY_NAME : value.trim();
    }
}
