package org.example.namelesschamber.domain.post.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.namelesschamber.common.exception.CustomException;
import org.example.namelesschamber.common.exception.ErrorCode;
import org.example.namelesschamber.domain.post.dto.request.PostCreateRequestDto;
import org.example.namelesschamber.domain.post.dto.response.PostCreateResponseDto;
import org.example.namelesschamber.domain.post.dto.response.PostDetailResponseDto;
import org.example.namelesschamber.domain.post.dto.response.PostPreviewListResponse;
import org.example.namelesschamber.domain.post.dto.response.PostPreviewResponseDto;
import org.example.namelesschamber.domain.post.entity.FirstWriteGate;
import org.example.namelesschamber.domain.post.entity.Post;
import org.example.namelesschamber.domain.post.entity.PostType;
import org.example.namelesschamber.domain.post.repository.PostRepository;
import org.example.namelesschamber.domain.readhistory.service.ReadHistoryService;
import org.example.namelesschamber.domain.user.service.CoinService;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final ReadHistoryService readHistoryService;
    private final CoinService coinService;
    private final CalendarService calendarService;
    private final MongoTemplate mongoTemplate;

    @Transactional(readOnly = true)
    public PostPreviewListResponse getPostPreviews(String userId) {
        int coin = coinService.getCoin(userId);
        List<PostPreviewResponseDto> posts = postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PostPreviewResponseDto::from)
                .toList();
        return PostPreviewListResponse.of(posts, coin);
    }

    @Transactional(readOnly = true)
    public PostPreviewListResponse getPostPreviews(PostType type, String userId) {
        int coin = coinService.getCoin(userId);
        List<PostPreviewResponseDto> posts = postRepository.findAllByTypeOrderByCreatedAtDesc(type).stream()
                .map(PostPreviewResponseDto::from)
                .toList();
        return PostPreviewListResponse.of(posts, coin);
    }

    public PostCreateResponseDto createPost(PostCreateRequestDto request, String userId) {
        request.type().validateContentLength(request.content());

        Post post = postRepository.save(Post.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .tags(request.tags())
                .userId(userId)
                .build());

        // 코인 지급 보상 트랜잭션
        int coin;
        try {
            coin = coinService.rewardForPost(userId, 1);
        } catch (RuntimeException ex) {
            try {
                postRepository.deleteById(post.getId());
            } catch (RuntimeException cleanupEx) {
                log.error("Compensation(delete post) failed. postId={}, userId={}", post.getId(), userId, cleanupEx);
            }
            throw ex;
        }

        boolean isFirstToday;
        try {
            isFirstToday = markFirstWriteIfAbsent(userId);
        } catch (RuntimeException ex) {
            log.warn("Failed to mark first write for user {}", userId, ex);
            isFirstToday = false;
        }

        int totalPosts = Math.toIntExact(postRepository.countByUserId(userId));

        PostCreateResponseDto.WeeklyCalendarDto calendar = null;

        if (isFirstToday) {
            try {
                calendar = calendarService.computeThisWeek(userId);
            } catch (RuntimeException ex) {
                log.warn("Weekly calendar compute failed. userId={}, postId={}", userId, post.getId(), ex);
            }
        }

        return new PostCreateResponseDto(
                post.getId(),
                totalPosts,
                coin,
                isFirstToday,
                calendar
        );
    }

    /** 오늘 첫 글 게이트 유니크 업서트. 최초만 true */
    private boolean markFirstWriteIfAbsent(String userId) {
        Instant dayStartUtc = LocalDate.now(KST).atStartOfDay(KST).toInstant();

        Query q = Query.query(
                Criteria.where("userId").is(userId)
                        .and("dayStart").is(dayStartUtc)
        );

        Update u = new Update()
                .setOnInsert("userId", userId)
                .setOnInsert("dayStart", dayStartUtc)
                .setOnInsert("createdAt", Instant.now());

        FindAndModifyOptions opt = FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(false);

        FirstWriteGate before = mongoTemplate.findAndModify(q, u, opt, FirstWriteGate.class);
        return (before == null);
    }

    public PostDetailResponseDto getPostById(String postId, String userId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        boolean charged = coinService.chargeIfEnough(userId, 1);

        if (!charged){
            log.warn("User {} does not have enough coins to read post {}", userId, postId);
            throw new CustomException(ErrorCode.NOT_ENOUGH_COIN);
        }

        boolean firstRead;
        try {
             firstRead = readHistoryService.record(userId, postId);
        } catch (RuntimeException ex) {
            coinService.refund(userId, 1);
            throw ex;
        }

        if (firstRead) {
            incrementViews(postId);
        } else {
            coinService.refund(userId, 1);
        }

        int finalCoin = coinService.getCoin(userId);

        return PostDetailResponseDto.from(post, finalCoin);

    }

    @Transactional(readOnly = true)
    public List<PostPreviewResponseDto> getMyPosts(String userId) {
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(PostPreviewResponseDto::from)
                .toList();
    }

    private void incrementViews(String postId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("views", 1),
                Post.class
        );
    }

}
