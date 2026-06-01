package org.example.moomyeongso.domain.postimage.service;

import org.example.moomyeongso.common.config.aws.S3StorageProperties;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.domain.post.entity.PostImageAttachment;
import org.example.moomyeongso.domain.postimage.entity.PostImage;
import org.example.moomyeongso.domain.postimage.entity.PostImageStatus;
import org.example.moomyeongso.domain.postimage.repository.PostImageRepository;
import org.example.moomyeongso.domain.postimage.storage.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostImageServiceTest {

    private PostImageRepository postImageRepository;
    private PostImageService postImageService;

    @BeforeEach
    void setUp() {
        postImageRepository = mock(PostImageRepository.class);
        PostImageProperties properties = new PostImageProperties();
        postImageService = new PostImageService(
                postImageRepository,
                mock(ImageProcessor.class),
                mock(ImageStorageService.class),
                properties,
                new S3StorageProperties()
        );
    }

    @Test
    void attachImagesKeepsRequestOrderAndMarksImagesAttached() {
        String userId = "user-1";
        String postId = "post-1";
        PostImage second = image("image-2");
        PostImage first = image("image-1");

        when(postImageRepository.findAllByIdInAndUserId(List.of("image-2", "image-1"), userId))
                .thenReturn(List.of(first, second));

        List<PostImageAttachment> attachments =
                postImageService.attachImages(userId, postId, List.of("image-2", "image-1"));

        assertThat(attachments).extracting(PostImageAttachment::getImageId)
                .containsExactly("image-2", "image-1");
        assertThat(attachments).extracting(PostImageAttachment::getSortOrder)
                .containsExactly(0, 1);
        assertThat(first.getStatus()).isEqualTo(PostImageStatus.ATTACHED);
        assertThat(second.getStatus()).isEqualTo(PostImageStatus.ATTACHED);
        assertThat(first.getPostId()).isEqualTo(postId);
        assertThat(second.getPostId()).isEqualTo(postId);
        verify(postImageRepository).saveAll(List.of(second, first));
    }

    @Test
    void attachImagesRejectsMoreThanFiveImages() {
        assertThatThrownBy(() -> postImageService.attachImages(
                "user-1",
                "post-1",
                List.of("1", "2", "3", "4", "5", "6")
        )).isInstanceOf(CustomException.class);

        verifyNoInteractions(postImageRepository);
    }

    @Test
    void attachImagesRejectsAlreadyAttachedImage() {
        String userId = "user-1";
        PostImage attached = PostImage.builder()
                .id("image-1")
                .userId(userId)
                .status(PostImageStatus.ATTACHED)
                .build();

        when(postImageRepository.findAllByIdInAndUserId(List.of("image-1"), userId))
                .thenReturn(List.of(attached));

        assertThatThrownBy(() -> postImageService.attachImages(userId, "post-1", List.of("image-1")))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void cleanupExpiredImagesQueriesLimitedBatches() {
        postImageService.cleanupExpiredImages();

        verify(postImageRepository).findAllByStatusAndDeleteAfterLessThanEqual(
                eq(PostImageStatus.DELETED),
                any(),
                any(Pageable.class)
        );
        verify(postImageRepository).findAllByStatusAndCreatedAtBefore(
                eq(PostImageStatus.TEMP),
                any(),
                any(Pageable.class)
        );
    }

    private PostImage image(String id) {
        return PostImage.builder()
                .id(id)
                .userId("user-1")
                .imageUrl("https://example.com/" + id + ".jpg")
                .thumbnailUrl("https://example.com/" + id + "-thumb.jpg")
                .status(PostImageStatus.TEMP)
                .build();
    }
}

