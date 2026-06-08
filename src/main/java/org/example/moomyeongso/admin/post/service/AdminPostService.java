package org.example.moomyeongso.admin.post.service;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.admin.post.dto.request.AdminPostRequestDto;
import org.example.moomyeongso.admin.post.dto.response.AdminPostResponseDto;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.postimage.service.PostImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;

    @Transactional(readOnly = true)
    public List<AdminPostResponseDto> getPosts(PostType type) {
        List<Post> posts =
                (type == null)
                        ? postRepository.findAllByStatusOrderByCreatedAtDesc(PostStatus.ACTIVE)
                        : postRepository.findAllByTypeAndStatusOrderByCreatedAtDesc(type, PostStatus.ACTIVE);

        return posts.stream()
                .map(AdminPostResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPostResponseDto getPostById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        return AdminPostResponseDto.from(post);
    }

    @Transactional("mongoTransactionManager")
    public void updatePost(String id, AdminPostRequestDto request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.updateByAdmin(request.content());
        postRepository.save(post);
    }

    @Transactional("mongoTransactionManager")
    public void deletePost(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.deleteByAdmin();
        postRepository.save(post);
        postImageService.markPostImagesDeleted(post.getId());
    }
}
