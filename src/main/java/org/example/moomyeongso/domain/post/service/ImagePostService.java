package org.example.moomyeongso.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.domain.post.dto.request.ImagePostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.postimage.dto.response.PostImageUploadResponseDto;
import org.example.moomyeongso.domain.postimage.dto.response.UploadedPostImageResponseDto;
import org.example.moomyeongso.domain.postimage.service.PostImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImagePostService {

    private final PostImageService postImageService;
    private final PostService postService;

    public PostCreateResponseDto createImagePost(
            ImagePostCreateRequestDto request,
            List<MultipartFile> images,
            String userId
    ) {
        PostImageUploadResponseDto uploadResponse = postImageService.uploadImages(images, userId);
        List<String> imageIds = uploadResponse.images().stream()
                .map(UploadedPostImageResponseDto::imageId)
                .toList();
        return postService.createImagePost(request, imageIds, userId);
    }
}
