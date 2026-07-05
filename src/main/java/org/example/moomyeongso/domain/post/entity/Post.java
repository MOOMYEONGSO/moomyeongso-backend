package org.example.moomyeongso.domain.post.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "status_tags_idx", def = "{'status': 1, 'tags': 1}"),
        @CompoundIndex(name = "status_id_desc_idx", def = "{'status': 1, '_id': -1}"),
        @CompoundIndex(name = "type_status_id_desc_idx", def = "{'type': 1, 'status': 1, '_id': -1}")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "content")
@EqualsAndHashCode(of = "id")
public class Post {

    public static final String DEFAULT_DISPLAY_NAME = "익명";

    @Id
    private String id;

    private String content;

    //멤버 식별용 ID
    @Indexed
    private String userId;

    private PostType type;

    @Builder.Default
    private String from = DEFAULT_DISPLAY_NAME;

    @Builder.Default
    private String to = DEFAULT_DISPLAY_NAME;

    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    // TODO: viewCount로 재정의 필요
    @Builder.Default
    private long views = 0L;

    // TODO: likeCount로 재정의 필요
    @Builder.Default
    private long likes = 0L;

    @Builder.Default
    private long commentCount = 0L;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<PostImageAttachment> images = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    public void updateByAdmin(String content) {
        this.content = content;
    }
    public void deleteByAdmin() {
        this.status = PostStatus.DELETED;
    }

    public void markPending() {
        this.status = PostStatus.PENDING;
    }

    public void markActive() {
        this.status = PostStatus.ACTIVE;
    }

    public void attachImages(List<PostImageAttachment> images) {
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
    }

    public String getFrom() {
        return normalizeDisplayName(from);
    }

    public String getTo() {
        return normalizeDisplayName(to);
    }

    private String normalizeDisplayName(String value) {
        return value == null || value.isBlank() ? DEFAULT_DISPLAY_NAME : value.trim();
    }
}
