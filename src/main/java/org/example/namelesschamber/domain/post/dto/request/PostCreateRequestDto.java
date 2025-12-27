package org.example.namelesschamber.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.namelesschamber.domain.post.entity.PostType;

import java.util.List;

public record PostCreateRequestDto(
        @NotBlank
        String title,
        @NotBlank
        String content,
        @NotNull
        PostType type,
        List<String> tags
) {
}
