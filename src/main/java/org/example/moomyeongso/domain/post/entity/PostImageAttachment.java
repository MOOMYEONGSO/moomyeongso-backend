package org.example.moomyeongso.domain.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImageAttachment {

    private String imageId;
    private String imageUrl;
    private String thumbnailUrl;
    private int sortOrder;
}

