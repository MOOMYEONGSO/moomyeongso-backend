package org.example.moomyeongso.domain.postimage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.response.ApiResponse;
import org.example.moomyeongso.domain.auth.core.SecurityUtils;
import org.example.moomyeongso.domain.postimage.dto.response.PostImageUploadResponseDto;
import org.example.moomyeongso.domain.postimage.service.PostImageService;
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

    private final PostImageService postImageService;

    @Operation(
            summary = "게시글 이미지 업로드",
            description = "게시글 작성 전에 이미지를 업로드합니다. jpg, png, webp 형식만 허용하며 최대 5장, 장당 10MB까지 업로드할 수 있습니다."
    )
    @PostMapping(value = "/post-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostImageUploadResponseDto>> uploadPostImages(
            @RequestPart("images") List<MultipartFile> images
    ) {
        String subject = SecurityUtils.getCurrentSubject();
        PostImageUploadResponseDto response = postImageService.uploadImages(images, subject);
        return ApiResponse.success(HttpStatus.CREATED, response);
    }
}

