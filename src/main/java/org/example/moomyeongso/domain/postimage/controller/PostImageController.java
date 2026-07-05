package org.example.moomyeongso.domain.postimage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.response.ApiResponse;
import org.example.moomyeongso.domain.auth.core.SecurityUtils;
import org.example.moomyeongso.domain.post.dto.request.ImagePostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.post.service.ImagePostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Post Images", description = "게시글 이미지 API")
public class PostImageController {

    private final ImagePostService imagePostService;

    @Operation(
            summary = "이미지 게시글 작성",
            description = "이미지만 업로드해 게시글 작성을 완료합니다. "
                    + "request 파트에는 선택 from/to/tags를 전달하고, "
                    + "images 파트에는 1~5개의 이미지를 전달합니다. "
                    + "게시글 type은 IMAGE로 저장됩니다."
    )
    @PostMapping(value = "/post-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponseDto>> createImagePost(
            @RequestPart("request") @Valid ImagePostCreateRequestDto request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        String subject = SecurityUtils.getCurrentSubject();
        PostCreateResponseDto response = imagePostService.createImagePost(request, images, subject);
        return ApiResponse.success(HttpStatus.CREATED, response);
    }
}
