package org.example.moomyeongso.domain.post.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "post_comments")
@CompoundIndex(name = "post_created_idx", def = "{'postId': 1, 'createdAt': 1}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class PostComment {

    @Id
    private String id;

    @Indexed
    private String postId;

    @Indexed
    private String authorId;

    private String authorNickname;

    private String content;

    @CreatedDate
    private Instant createdAt;
}
