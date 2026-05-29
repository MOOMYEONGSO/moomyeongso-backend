package org.example.moomyeongso.domain.postimage.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.entity.PostImageAttachment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "postImages")
@CompoundIndexes({
        @CompoundIndex(name = "user_status_created_idx", def = "{'userId': 1, 'status': 1, 'createdAt': 1}"),
        @CompoundIndex(name = "status_delete_after_idx", def = "{'status': 1, 'deleteAfter': 1}"),
        @CompoundIndex(name = "post_status_idx", def = "{'postId': 1, 'status': 1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PostImage {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String postId;

    private String storageKey;

    private String thumbnailStorageKey;

    private String imageUrl;

    private String thumbnailUrl;

    private String contentType;

    private long size;

    private int width;

    private int height;

    @Builder.Default
    private PostImageStatus status = PostImageStatus.TEMP;

    @CreatedDate
    private Instant createdAt;

    private Instant attachedAt;

    private Instant deletedAt;

    private Instant deleteAfter;

    public void attach(String postId) {
        if (status != PostImageStatus.TEMP) {
            throw new CustomException(ErrorCode.IMAGE_ALREADY_ATTACHED);
        }
        this.postId = postId;
        this.status = PostImageStatus.ATTACHED;
        this.attachedAt = Instant.now();
    }

    public void markDeleted(Instant deleteAfter) {
        this.status = PostImageStatus.DELETED;
        this.deletedAt = Instant.now();
        this.deleteAfter = deleteAfter;
    }

    public PostImageAttachment toAttachment(int sortOrder) {
        return PostImageAttachment.builder()
                .imageId(id)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .sortOrder(sortOrder)
                .build();
    }
}

