package org.example.moomyeongso.domain.post.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.moomyeongso.domain.post.entity.PostType;

import java.util.List;

public record ImagePostCreateRequestDto(
        @NotNull
        PostType type,
        List<String> tags
) {
    public ImagePostCreateRequestDto {
        tags = tags == null ? List.of() : tags;
    }
}
