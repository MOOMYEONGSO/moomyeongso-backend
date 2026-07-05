package org.example.moomyeongso.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.dto.request.PostCommentCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.request.PostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.request.ImagePostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCommentCreateResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostCommentResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostDetailResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewCursorListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewResponseDto;
import org.example.moomyeongso.domain.post.entity.FirstWriteGate;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostComment;
import org.example.moomyeongso.domain.post.entity.PostCommentStatus;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostTag;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostCommentRepository;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.post.repository.RandomPostFinder;
import org.example.moomyeongso.domain.postimage.service.PostImageService;
import org.example.moomyeongso.domain.readhistory.service.ReadHistoryService;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.example.moomyeongso.domain.user.service.CoinService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DuplicateKeyException;
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
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private static final int DEFAULT_POST_PREVIEW_LIMIT = 20;
    private static final int MAX_POST_PREVIEW_LIMIT = 100;

    private final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final PostRepository postRepository;
    private final ReadHistoryService readHistoryService;
    private final CoinService coinService;
    private final CalendarService calendarService;
    private final MongoTemplate mongoTemplate;
    private final RandomPostFinder randomPostFinder;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;
    private final PostCommentService postCommentService;
    private final PostImageService postImageService;

    public PostPreviewCursorListResponse getPostPreviews(String userId) {
        return getPostPreviews(userId, null, DEFAULT_POST_PREVIEW_LIMIT);
    }

    public PostPreviewCursorListResponse getPostPreviews(String userId, String cursor, int limit) {
        return getPostPreviews(null, userId, cursor, limit);
    }

    public PostPreviewCursorListResponse getPostPreviews(PostType type, String userId) {
        return getPostPreviews(type, userId, null, DEFAULT_POST_PREVIEW_LIMIT);
    }

    public PostPreviewCursorListResponse getPostPreviews(PostType type, String userId, String cursor, int limit) {
        validatePostPreviewLimit(limit);

        int coin = coinService.getCoin(userId);
        List<Post> fetchedPosts = fetchPostPreviewPage(type, userId, normalizeCursor(cursor), limit);
        boolean hasNext = fetchedPosts.size() > limit;
        List<Post> postEntities = hasNext ? fetchedPosts.subList(0, limit) : fetchedPosts;
        Map<String, Long> commentCounts = postCommentService.getActiveCommentCounts(
                postEntities.stream().map(Post::getId).toList()
        );
        List<PostPreviewResponseDto> posts = postEntities.stream()
                .map(post -> PostPreviewResponseDto.from(post, commentCounts.getOrDefault(post.getId(), 0L)))
                .toList();
        return PostPreviewCursorListResponse.of(posts, coin, hasNext ? resolveNextCursor(posts) : null);
    }

    private void validatePostPreviewLimit(int limit) {
        if (limit <= 0 || limit > MAX_POST_PREVIEW_LIMIT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private List<Post> fetchPostPreviewPage(PostType type, String userId, String cursor, int limit) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        if (type == null) {
            return cursor == null
                    ? postRepository.findAllByStatusAndUserIdNotOrderByIdDesc(PostStatus.ACTIVE, userId, pageable)
                    : postRepository.findAllByStatusAndUserIdNotAndIdLessThanOrderByIdDesc(PostStatus.ACTIVE, userId, cursor, pageable);
        }

        return cursor == null
                ? postRepository.findAllByTypeAndStatusAndUserIdNotOrderByIdDesc(type, PostStatus.ACTIVE, userId, pageable)
                : postRepository.findAllByTypeAndStatusAndUserIdNotAndIdLessThanOrderByIdDesc(type, PostStatus.ACTIVE, userId, cursor, pageable);
    }

    private String normalizeCursor(String cursor) {
        return cursor == null || cursor.isBlank() ? null : cursor;
    }

    private String resolveNextCursor(List<PostPreviewResponseDto> posts) {
        return posts.isEmpty() ? null : posts.get(posts.size() - 1).postId();
    }

    @Transactional("mongoTransactionManager")
    public PostCreateResponseDto createPost(PostCreateRequestDto request, String userId) {
        PostType.TEXT.validateContentLength(request.content());
        return createPost(request.content(), PostType.TEXT, request.from(), request.to(), request.tags(), List.of(), userId);
    }

    @Transactional("mongoTransactionManager")
    public PostCreateResponseDto createImagePost(ImagePostCreateRequestDto request, List<String> imageIds, String userId) {
        if (imageIds == null || imageIds.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return createPost("", PostType.IMAGE, request.from(), request.to(), request.tags(), imageIds, userId);
    }

    private PostCreateResponseDto createPost(
            String content,
            PostType type,
            String from,
            String to,
            List<String> tags,
            List<String> imageIds,
            String userId
    ) {
        int coin = coinService.rewardForPost(userId, 1);

        Post post = postRepository.save(Post.builder()
                .content(content)
                .type(type)
                .from(from)
                .to(to)
                .tags(tags)
                .userId(userId)
                .build());

        post.attachImages(postImageService.attachImages(userId, post.getId(), imageIds));
        postRepository.save(post);

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

        Post post = getActivePost(postId);

        boolean isOwner = userId.equals(post.getUserId());

        boolean firstRead = readHistoryService.record(userId, postId);

        if (firstRead && !isOwner) {
            boolean chargedCoin = coinService.chargeIfEnough(userId, 1);
            if (!chargedCoin) {
                log.warn("User {} does not have enough coins to read post {}", userId, postId);
                throw new CustomException(ErrorCode.NOT_ENOUGH_COIN);
            }
        }

//        if (firstRead) {
//            incrementViews(postId);
//        }
        // 조회수 무조건 증가로 변경 + 증가된 값을 같은 응답에 반영
        Post updatedPost = incrementViewsAndGetPost(postId);

        int finalCoin = coinService.getCoin(userId);
        List<PostCommentResponseDto> comments = getPostComments(postId, userId);

        return PostDetailResponseDto.from(updatedPost, finalCoin, comments);

    }

    @Transactional("mongoTransactionManager")
    public PostCommentCreateResponseDto createComment(String postId, PostCommentCreateRequestDto request, String userId) {
        Post post = getActivePost(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (postCommentRepository.existsByPostIdAndAuthorIdAndStatus(post.getId(), userId, PostCommentStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.ALREADY_COMMENTED);
        }

        String nickname = resolveNickname(user);

        PostComment comment;
        try {
            comment = postCommentRepository.save(PostComment.builder()
                    .postId(post.getId())
                    .authorId(userId)
                    .authorNickname(nickname)
                    .content(request.content())
                    .build());
        } catch (DuplicateKeyException ex) {
            throw new CustomException(ErrorCode.ALREADY_COMMENTED);
        }

        syncCommentCount(post.getId());
        return PostCommentCreateResponseDto.from(comment);
    }

    @Transactional("mongoTransactionManager")
    public void deleteComment(String postId, String commentId, String userId) {
        getActivePost(postId);

        PostComment comment = postCommentRepository.findByIdAndPostIdAndStatus(commentId, postId, PostCommentStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        comment.markDeleted();
        postCommentRepository.save(comment);
        syncCommentCount(postId);
    }

    public List<PostPreviewResponseDto> getMyPosts(String userId, PostType type) {
        List<Post> posts = (type == null)
                ? postRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, PostStatus.ACTIVE)
                : postRepository.findAllByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, type, PostStatus.ACTIVE);

        Map<String, Long> commentCounts = postCommentService.getActiveCommentCounts(
                posts.stream().map(Post::getId).toList()
        );
        return posts.stream()
                .map(post -> PostPreviewResponseDto.from(post, commentCounts.getOrDefault(post.getId(), 0L)))
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
        Map<String, Long> commentCounts = postCommentService.getActiveCommentCounts(
                posts.stream().map(Post::getId).toList()
        );
        return posts.stream()
                .map(post -> PostPreviewResponseDto.from(post, commentCounts.getOrDefault(post.getId(), 0L)))
                .toList();
    }

    private String selectTagByPriority(List<String> tags, int index) {
        if (index < 0 || tags == null || tags.isEmpty()) {
            return null;
        }

        List<String> sorted = PostTag.sortByPriority(tags);
        return index < sorted.size() ? sorted.get(index) : null;
    }

    private Post incrementViewsAndGetPost(String postId) {
        Query query = Query.query(
                Criteria.where("_id").is(postId)
                        .and("status").is(PostStatus.ACTIVE)
        );
        Post updatedPost = mongoTemplate.findAndModify(
                query,
                new Update().inc("views", 1),
                FindAndModifyOptions.options().returnNew(true),
                Post.class
        );

        if (updatedPost == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return updatedPost;
    }

    private void syncCommentCount(String postId) {
        long activeCommentCount = postCommentRepository.countByPostIdAndStatus(postId, PostCommentStatus.ACTIVE);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(postId)),
                new Update().set("commentCount", activeCommentCount),
                Post.class
        );
    }

    private Post getActivePost(String postId) {
        return postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private List<PostCommentResponseDto> getPostComments(String postId, String userId) {
        return postCommentRepository.findAllByPostIdAndStatusOrderByCreatedAtAsc(postId, PostCommentStatus.ACTIVE).stream()
                .map(comment -> PostCommentResponseDto.from(comment, userId))
                .toList();
    }

    private String resolveNickname(User user) {
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            return user.getId();
        }
        return user.getNickname();
    }

}
