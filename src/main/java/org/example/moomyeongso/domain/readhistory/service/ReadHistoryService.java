package org.example.moomyeongso.domain.readhistory.service;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewResponseDto;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.post.service.PostCommentService;
import org.example.moomyeongso.domain.readhistory.entity.ReadHistory;
import org.example.moomyeongso.domain.readhistory.repository.ReadHistoryRepository;
import org.example.moomyeongso.domain.user.service.CoinService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReadHistoryService {

    private final ReadHistoryRepository readHistoryRepository;
    private final PostRepository postRepository;
    private final CoinService coinService;
    private final MongoTemplate mongoTemplate;
    private final PostCommentService postCommentService;

    /**
     * 내가 열람한 일기 목록 조회
     */
    @Transactional(readOnly = true)
    public PostPreviewListResponse getMyReadPosts(String userId) {
        return getMyReadPosts(userId, null);
    }

    /**
     * 내가 열람한 일기 목록 조회(타입 필터)
     */
    @Transactional(readOnly = true)
    public PostPreviewListResponse getMyReadPosts(String userId, PostType type) {
        int coin = coinService.getCoin(userId);

        List<ReadHistory> histories = readHistoryRepository.findAllByUserIdOrderByReadAtDesc(userId);

        List<String> postIds = histories.stream()
                .map(ReadHistory::getPostId)
                .toList();

        Map<String, Post> postMap = postRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> filteredPosts = histories.stream()
                .map(history -> postMap.get(history.getPostId()))
                .filter(Objects::nonNull)
                .filter(post -> post.getStatus() == PostStatus.ACTIVE)
                .filter(post -> type == null || post.getType() == type)
                .toList();

        Map<String, Long> commentCounts = postCommentService.getActiveCommentCounts(
                filteredPosts.stream().map(Post::getId).toList()
        );

        List<PostPreviewResponseDto> posts = filteredPosts.stream()
                .map(post -> PostPreviewResponseDto.from(post, commentCounts.getOrDefault(post.getId(), 0L)))
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
        Query q = Query.query(Criteria.where("userId").is(userId).and("postId").is(postId));
        LocalDateTime now = LocalDateTime.now();
        Update u = new Update()
                .setOnInsert("userId", userId)
                .setOnInsert("postId", postId)
                .set("readAt", now);
        return mongoTemplate.upsert(q, u, ReadHistory.class).getUpsertedId() != null;
    }
}
