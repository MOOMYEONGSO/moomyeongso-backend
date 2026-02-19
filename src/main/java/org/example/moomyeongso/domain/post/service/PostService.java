package org.example.moomyeongso.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.dto.request.PostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostDetailResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewResponseDto;
import org.example.moomyeongso.domain.post.entity.FirstWriteGate;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostTag;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.post.repository.RandomPostFinder;
import org.example.moomyeongso.domain.readhistory.service.ReadHistoryService;
import org.example.moomyeongso.domain.user.service.CoinService;
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
    private final RandomPostFinder randomPostFinder;

    public PostPreviewListResponse getPostPreviews(String userId) {
        int coin = coinService.getCoin(userId);
        List<PostPreviewResponseDto> posts =
                postRepository.findAllByStatusAndUserIdNotOrderByCreatedAtDesc(PostStatus.ACTIVE, userId).stream()
                        .map(PostPreviewResponseDto::from)
                        .toList();
        return PostPreviewListResponse.of(posts, coin);
    }

    public PostPreviewListResponse getPostPreviews(PostType type, String userId) {
        int coin = coinService.getCoin(userId);
        List<PostPreviewResponseDto> posts =
                postRepository.findAllByTypeAndStatusAndUserIdNotOrderByCreatedAtDesc(type, PostStatus.ACTIVE, userId).stream()
                        .map(PostPreviewResponseDto::from)
                        .toList();
        return PostPreviewListResponse.of(posts, coin);
    }

    @Transactional("mongoTransactionManager")
    public PostCreateResponseDto createPost(PostCreateRequestDto request, String userId) {
        request.type().validateContentLength(request.content());

        int coin = coinService.rewardForPost(userId, 1);

        Post post = postRepository.save(Post.builder()
                .title(request.title())
                .content(request.content())
                .type(request.type())
                .tags(request.tags())
                .userId(userId)
                .status(PostStatus.ACTIVE)
                .build());

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

    @Transactional("mongoTransactionManager")
    public PostDetailResponseDto getPostById(String postId, String userId) {

        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        boolean isOwner = userId.equals(post.getUserId());

        boolean firstRead = readHistoryService.record(userId, postId);

        if (firstRead && !isOwner) {
            boolean chargedCoin = coinService.chargeIfEnough(userId, 1);
            if (!chargedCoin) {
                log.warn("User {} does not have enough coins to read post {}", userId, postId);
                throw new CustomException(ErrorCode.NOT_ENOUGH_COIN);
            }
        }

        if (firstRead) {
            incrementViews(postId);
        }

        int finalCoin = coinService.getCoin(userId);

        return PostDetailResponseDto.from(post, finalCoin);

    }

    public List<PostPreviewResponseDto> getMyPosts(String userId) {
        return postRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, PostStatus.ACTIVE).stream()
                .map(PostPreviewResponseDto::from)
                .toList();
    }

    public PostPreviewListResponse getRandomPostPreviews(int count, List<String> tags, int reroll, String userId) {
        if (count <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        int coin = coinService.getCoin(userId);
        String selectedTag = selectTagForStep(tags, reroll);
        List<PostPreviewResponseDto> posts = fetchRandomPostPreviews(count, selectedTag, userId);
        return PostPreviewListResponse.of(posts, coin);
    }

    private String selectTagForStep(List<String> tags, int reroll) {
        int index = reroll <= 0 ? 0 : reroll == 1 ? 1 : -1;
        return selectTagByPriority(tags, index);
    }

    private List<PostPreviewResponseDto> fetchRandomPostPreviews(int count, String tag, String userId) {
        List<Post> posts;
        if (tag == null) {
            posts = randomPostFinder.findRandomByStatusExcludingUser(PostStatus.ACTIVE, count, userId);
        } else {
            posts = randomPostFinder.findRandomByStatusAndTagExcludingUser(PostStatus.ACTIVE, tag, count, userId);
        }
        return posts.stream()
                .map(PostPreviewResponseDto::from)
                .toList();
    }

    private String selectTagByPriority(List<String> tags, int index) {
        if (index < 0 || tags == null || tags.isEmpty()) {
            return null;
        }

        List<String> sorted = PostTag.sortByPriority(tags);
        return index < sorted.size() ? sorted.get(index) : null;
    }

    private void incrementViews(String postId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().inc("views", 1),
                Post.class
        );
    }

}
