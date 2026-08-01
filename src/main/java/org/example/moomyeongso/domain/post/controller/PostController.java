package org.example.moomyeongso.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.response.ApiResponse;
import org.example.moomyeongso.domain.auth.core.SecurityUtils;
import org.example.moomyeongso.domain.post.dto.request.PostCommentCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.request.PostCreateRequestDto;
import org.example.moomyeongso.domain.post.dto.response.PostCommentCreateResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostCreateResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostDetailResponseDto;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewCursorListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewListResponse;
import org.example.moomyeongso.domain.post.dto.response.PostPreviewResponseDto;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.service.PostService;
import org.example.moomyeongso.domain.readhistory.service.ReadHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@Tag(name = "Posts", description = "게시글 API")
public class PostController {

    private final PostService postService;
    private final ReadHistoryService readHistoryService;

    @Operation(
            summary = "글 조회",
            description = "게시물의 미리보기 리스트를 최신순으로 반환합니다. 'type' 쿼리 파라미터로 특정 타입의 글만 조회할 수 있고, cursor에는 마지막으로 로드한 postId를 전달합니다."
    )
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostPreviewCursorListResponse>> getPosts(
            @RequestParam(required = false) PostType type,
            @Parameter(description = "마지막으로 로드한 게시물의 postId")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "가져올 글 수")
            @RequestParam(defaultValue = "20") int limit) {

        String userId = SecurityUtils.getCurrentSubject();

        PostPreviewCursorListResponse response =
                (type == null)
                        ? postService.getPostPreviews(userId, cursor, limit)
                        : postService.getPostPreviews(type, userId, cursor, limit);

        return ApiResponse.success(HttpStatus.OK, response);
    }

    @Operation(
            summary = "무작위 글 조회",
            description = "태그가 있으면 해당 태그 중 하나를 포함한 글을 먼저 추천하고, 나머지는 전체 글에서 무작위로 추천합니다. 태그가 없으면 전체 글에서 무작위로 추천합니다."
    )
    @GetMapping("/posts/random")
    public ResponseEntity<ApiResponse<PostPreviewListResponse>> getRandomPosts(
            @Parameter(description = "추천에 사용할 태그 목록")
            @RequestParam(required = false) List<String> tags) {

        String userId = SecurityUtils.getCurrentSubject();
        PostPreviewListResponse response =
                postService.getRandomPostPreviews(tags, userId);
        return ApiResponse.success(HttpStatus.OK, response);
    }
    @Operation(
            summary = "글 작성",
            description = "새로운 텍스트 게시글을 작성합니다. type은 TEXT로 저장되고, from/to가 비어 있으면 익명으로 저장됩니다."
    )
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostCreateResponseDto>> createPost(
            @RequestBody @Valid PostCreateRequestDto request) {

        String subject = SecurityUtils.getCurrentSubject();

        PostCreateResponseDto response = postService.createPost(request, subject);
        return ApiResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(summary = "특정 글 조회", description = "게시글 ID로 특정 게시글의 상세 내용을 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponseDto>> getPostById(@PathVariable String postId) {

        String subject = SecurityUtils.getCurrentSubject();

        PostDetailResponseDto response = postService.getPostById(postId, subject);
        return ApiResponse.success(HttpStatus.OK, response);
    }

    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글을 작성합니다.")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PostCommentCreateResponseDto>> createComment(
            @PathVariable String postId,
            @RequestBody @Valid PostCommentCreateRequestDto request
    ) {
        String subject = SecurityUtils.getCurrentSubject();
        PostCommentCreateResponseDto response = postService.createComment(postId, request, subject);
        return ApiResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(summary = "댓글 삭제", description = "댓글 작성자 본인만 댓글을 삭제할 수 있습니다.")
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        String subject = SecurityUtils.getCurrentSubject();
        postService.deleteComment(postId, commentId, subject);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Operation(summary = "내가 쓴 글 조회", description = "내가 작성한 게시글 목록을 반환합니다. type 파라미터로 타입 필터링이 가능합니다.")
    @GetMapping("/posts/me")
    public ResponseEntity<ApiResponse<List<PostPreviewResponseDto>>> getMyPosts(
            @RequestParam(required = false) PostType type) {

        String subject = SecurityUtils.getCurrentSubject();

        List<PostPreviewResponseDto> response = postService.getMyPosts(subject, type);
        return ApiResponse.success(HttpStatus.OK,response);
    }

    @Operation(summary = "내가 열람한 글 조회", description = "내가 열람한 게시글 목록을 최신순으로 반환합니다. type 파라미터로 타입 필터링이 가능합니다.")
    @GetMapping("/posts/me/read")
    public ResponseEntity<ApiResponse<PostPreviewListResponse>> getMyReadPosts(
            @RequestParam(required = false) PostType type) {
        String subject = SecurityUtils.getCurrentSubject();
        PostPreviewListResponse response = readHistoryService.getMyReadPosts(subject, type);
        return ApiResponse.success(HttpStatus.OK, response);
    }

}
