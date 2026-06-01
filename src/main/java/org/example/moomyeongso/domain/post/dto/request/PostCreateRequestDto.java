package org.example.moomyeongso.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.moomyeongso.domain.post.entity.PostType;

import java.util.List;

public record PostCreateRequestDto(
        @NotBlank
        String title,
        @NotBlank
        String content,
        @NotNull
        PostType type,
        List<String> tags,
        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
        List<String> imageIds
) {
    public PostCreateRequestDto {
        tags = tags == null ? List.of() : tags;
        imageIds = imageIds == null ? List.of() : imageIds;
    }
}
