package org.example.namelesschamber.domain.readhistory.service;

import org.springframework.dao.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.example.namelesschamber.domain.post.dto.response.PostPreviewListResponse;
import org.example.namelesschamber.domain.post.dto.response.PostPreviewResponseDto;
import org.example.namelesschamber.domain.post.entity.Post;
import org.example.namelesschamber.domain.post.entity.PostStatus;
import org.example.namelesschamber.domain.post.repository.PostRepository;
import org.example.namelesschamber.domain.readhistory.entity.ReadHistory;
import org.example.namelesschamber.domain.readhistory.repository.ReadHistoryRepository;
import org.example.namelesschamber.domain.user.service.CoinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadHistoryService {

    private final ReadHistoryRepository readHistoryRepository;
    private final PostRepository postRepository;
    private final CoinService coinService;

    /**
     * 내가 열람한 일기 목록 조회
     */
    @Transactional(readOnly = true)
    public PostPreviewListResponse getMyReadPosts(String userId) {
        int coin = coinService.getCoin(userId);

        List<ReadHistory> histories = readHistoryRepository.findAllByUserIdOrderByReadAtDesc(userId);

        List<String> postIds = histories.stream()
                .map(ReadHistory::getPostId)
                .toList();

        Map<String, Post> postMap = postRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<PostPreviewResponseDto> posts = histories.stream()
                .map(history -> postMap.get(history.getPostId()))
                .filter(Objects::nonNull)
                .filter(post -> post.getStatus() == PostStatus.ACTIVE)
                .map(PostPreviewResponseDto::from)
                .toList();

        return PostPreviewListResponse.of(posts, coin);
    }

    /**
     * 특정 게시글을 이미 열람했는지 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyRead(String userId, String postId) {
        return readHistoryRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * 열람 기록 저장
     */
    public boolean record(String userId, String postId) {
        try {
            readHistoryRepository.save(ReadHistory.of(userId, postId));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
