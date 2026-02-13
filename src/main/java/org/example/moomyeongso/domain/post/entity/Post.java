package org.example.moomyeongso.domain.post.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@CompoundIndex(name = "status_tags_idx", def = "{'status': 1, 'tags': 1}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "content")
@EqualsAndHashCode(of = "id")
public class Post {

    @Id
    private String id;

    private String title;

    private String content;

    //멤버 식별용 ID
    @Indexed
    private String userId;

    private PostType type;

    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @Builder.Default
    private long views = 0L;

    @Builder.Default
    private long likes = 0L;
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    public void updateByAdmin(String title, String content) {
        this.title = title;
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
}
