package org.example.moomyeongso.domain.postimage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.response.ApiResponse;
import org.example.moomyeongso.domain.auth.core.SecurityUtils;
import org.example.moomyeongso.domain.post.dto.request.PostPhotoCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.post.service.PostPhotoService;
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

    private final PostPhotoService postPhotoService;

    @Operation(
            summary = "사진 게시글 작성",
            description = "사진만 업로드해 게시글 작성을 완료합니다. request 파트에는 type과 선택 tags를 전달하고, images 파트에는 1~5개의 이미지를 전달합니다."
    )
    @PostMapping(value = "/post-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCreateResponseDto>> createPhotoPost(
            @RequestPart("request") @Valid PostPhotoCreateRequestDto request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        String subject = SecurityUtils.getCurrentSubject();
        PostCreateResponseDto response = postPhotoService.createPhotoPost(request, images, subject);
        return ApiResponse.success(HttpStatus.CREATED, response);
    }
}
