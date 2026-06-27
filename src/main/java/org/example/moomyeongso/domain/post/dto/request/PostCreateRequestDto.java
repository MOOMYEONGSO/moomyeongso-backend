package org.example.moomyeongso.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.moomyeongso.domain.post.entity.Post;

import java.util.List;

public record PostCreateRequestDto(
        @Size(max = 30)
        String from,
        @Size(max = 30)
        String to,
        @NotBlank
        String content,
        List<String> tags
) {
    public PostCreateRequestDto {
        from = normalizeDisplayName(from);
        to = normalizeDisplayName(to);
        tags = tags == null ? List.of() : tags;
    }

    private static String normalizeDisplayName(String value) {
        return value == null || value.isBlank() ? Post.DEFAULT_DISPLAY_NAME : value.trim();
    }
}
