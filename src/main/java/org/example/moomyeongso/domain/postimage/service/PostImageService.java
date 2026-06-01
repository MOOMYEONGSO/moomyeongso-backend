package org.example.moomyeongso.domain.postimage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.common.config.aws.S3StorageProperties;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.entity.PostImageAttachment;
import org.example.moomyeongso.domain.postimage.dto.response.PostImageUploadResponseDto;
import org.example.moomyeongso.domain.postimage.dto.response.UploadedPostImageResponseDto;
import org.example.moomyeongso.domain.postimage.entity.PostImage;
import org.example.moomyeongso.domain.postimage.entity.PostImageStatus;
import org.example.moomyeongso.domain.postimage.repository.PostImageRepository;
import org.example.moomyeongso.domain.postimage.storage.ImageStorageService;
import org.example.moomyeongso.domain.postimage.storage.StoredImageObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageService {

    private static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";
    private static final String THUMBNAIL_EXTENSION = "jpg";
    private static final int CLEANUP_BATCH_SIZE = 100;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostImageRepository postImageRepository;
    private final ImageProcessor imageProcessor;
    private final ImageStorageService imageStorageService;
    private final PostImageProperties postImageProperties;
    private final S3StorageProperties s3StorageProperties;

    @Transactional("mongoTransactionManager")
    public PostImageUploadResponseDto uploadImages(List<MultipartFile> files, String userId) {
        validateUploadFiles(files);

        List<PostImage> uploadedImages = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploadedImages.add(uploadOne(file, userId));
            }
        } catch (RuntimeException ex) {
            uploadedImages.forEach(this::deleteObjectsQuietly);
            throw ex;
        }

        List<UploadedPostImageResponseDto> images = uploadedImages.stream()
                .map(UploadedPostImageResponseDto::from)
                .toList();
        return PostImageUploadResponseDto.of(images);
    }

    @Transactional("mongoTransactionManager")
    public List<PostImageAttachment> attachImages(String userId, String postId, List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }
        validateImageCount(imageIds.size());

        List<String> distinctImageIds = new ArrayList<>(new LinkedHashSet<>(imageIds));
        if (distinctImageIds.size() != imageIds.size()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Map<String, PostImage> imagesById = postImageRepository.findAllByIdInAndUserId(distinctImageIds, userId)
                .stream()
                .collect(Collectors.toMap(PostImage::getId, Function.identity()));

        List<PostImage> orderedImages = distinctImageIds.stream()
                .map(imageId -> resolveAttachableImage(imagesById, imageId))
                .toList();

        for (PostImage image : orderedImages) {
            image.attach(postId);
        }
        postImageRepository.saveAll(orderedImages);

        List<PostImageAttachment> attachments = new ArrayList<>();
        for (int i = 0; i < orderedImages.size(); i++) {
            attachments.add(orderedImages.get(i).toAttachment(i));
        }
        return attachments;
    }

    @Transactional("mongoTransactionManager")
    public void markPostImagesDeleted(String postId) {
        Instant deleteAfter = Instant.now().plusSeconds(postImageProperties.getDeletedRetentionDays() * 24 * 60 * 60);
        List<PostImage> images = postImageRepository.findAllByPostIdAndStatus(postId, PostImageStatus.ATTACHED);
        images.forEach(image -> image.markDeleted(deleteAfter));
        postImageRepository.saveAll(images);
    }

    public void cleanupExpiredImages() {
        cleanupDeletedImages();
        cleanupTemporaryImages();
    }

    private PostImage uploadOne(MultipartFile file, String userId) {
        validateFile(file);
        ProcessedImage processedImage = imageProcessor.process(file);

        String baseKey = buildBaseKey(userId);
        String originalKey = s3StorageProperties.getS3().getOriginalPrefix() + "/" + baseKey + "." + processedImage.format().extension();
        String thumbnailKey = s3StorageProperties.getS3().getThumbnailPrefix() + "/" + baseKey + "." + THUMBNAIL_EXTENSION;

        StoredImageObject original = null;
        StoredImageObject thumbnail = null;
        try {
            original = imageStorageService.upload(
                    processedImage.originalBytes(),
                    originalKey,
                    processedImage.format().contentType()
            );
            thumbnail = imageStorageService.upload(
                    processedImage.thumbnailBytes(),
                    thumbnailKey,
                    THUMBNAIL_CONTENT_TYPE
            );

            return postImageRepository.save(PostImage.builder()
                    .userId(userId)
                    .storageKey(original.key())
                    .thumbnailStorageKey(thumbnail.key())
                    .imageUrl(original.url())
                    .thumbnailUrl(thumbnail.url())
                    .contentType(processedImage.format().contentType())
                    .size(file.getSize())
                    .width(processedImage.width())
                    .height(processedImage.height())
                    .build());
        } catch (RuntimeException ex) {
            deleteQuietly(original);
            deleteQuietly(thumbnail);
            if (ex instanceof CustomException customException) {
                throw customException;
            }
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private void cleanupDeletedImages() {
        Instant now = Instant.now();
        List<PostImage> expiredImages = postImageRepository.findAllByStatusAndDeleteAfterLessThanEqual(
                PostImageStatus.DELETED,
                now,
                PageRequest.of(0, CLEANUP_BATCH_SIZE)
        );
        deleteObjectsAndDocuments(expiredImages);
    }

    private void cleanupTemporaryImages() {
        Instant cutoff = Instant.now().minusSeconds(postImageProperties.getTemporaryRetentionHours() * 60 * 60);
        List<PostImage> temporaryImages = postImageRepository.findAllByStatusAndCreatedAtBefore(
                PostImageStatus.TEMP,
                cutoff,
                PageRequest.of(0, CLEANUP_BATCH_SIZE)
        );
        deleteObjectsAndDocuments(temporaryImages);
    }

    private void deleteObjectsAndDocuments(List<PostImage> images) {
        for (PostImage image : images) {
            try {
                imageStorageService.delete(image.getStorageKey());
                imageStorageService.delete(image.getThumbnailStorageKey());
                postImageRepository.delete(image);
            } catch (RuntimeException ex) {
                log.warn("Post image cleanup failed. imageId={}, postId={}", image.getId(), image.getPostId(), ex);
            }
        }
    }

    private PostImage resolveAttachableImage(Map<String, PostImage> imagesById, String imageId) {
        PostImage image = imagesById.get(imageId);
        if (image == null) {
            throw new CustomException(ErrorCode.POST_IMAGE_NOT_FOUND);
        }
        if (image.getStatus() != PostImageStatus.TEMP) {
            throw new CustomException(ErrorCode.IMAGE_ALREADY_ATTACHED);
        }
        return image;
    }

    private void validateUploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validateImageCount(files.size());
    }

    private void validateImageCount(int count) {
        if (count > postImageProperties.getMaxImagesPerPost()) {
            throw new CustomException(ErrorCode.TOO_MANY_POST_IMAGES);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
        if (file.getSize() > postImageProperties.getMaxFileSizeBytes()) {
            throw new CustomException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String buildBaseKey(String userId) {
        LocalDate today = LocalDate.now(KST);
        return userId + "/" + today.getYear() + "/" + pad(today.getMonthValue()) + "/" + pad(today.getDayOfMonth())
                + "/" + UUID.randomUUID();
    }

    private String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private void deleteQuietly(StoredImageObject object) {
        if (object == null) {
            return;
        }
        try {
            imageStorageService.delete(object.key());
        } catch (RuntimeException ignored) {
            log.warn("Failed to rollback uploaded image object. key={}", object.key());
        }
    }

    private void deleteObjectsQuietly(PostImage image) {
        try {
            imageStorageService.delete(image.getStorageKey());
            imageStorageService.delete(image.getThumbnailStorageKey());
        } catch (RuntimeException ex) {
            log.warn("Failed to rollback uploaded image objects. imageId={}", image.getId(), ex);
        }
    }
}
