package org.example.namelesschamber.admin.post.service;

import lombok.RequiredArgsConstructor;
import org.example.namelesschamber.admin.post.dto.request.AdminPostRequestDto;
import org.example.namelesschamber.admin.post.dto.response.AdminPostResponseDto;
import org.example.namelesschamber.common.exception.CustomException;
import org.example.namelesschamber.common.exception.ErrorCode;
import org.example.namelesschamber.domain.post.entity.Post;
import org.example.namelesschamber.domain.post.entity.PostStatus;
import org.example.namelesschamber.domain.post.entity.PostType;
import org.example.namelesschamber.domain.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;

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
        post.updateByAdmin(request.title(), request.content());
        postRepository.save(post);
    }

    @Transactional("mongoTransactionManager")
    public void deletePost(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.deleteByAdmin();
        postRepository.save(post);
    }
}
