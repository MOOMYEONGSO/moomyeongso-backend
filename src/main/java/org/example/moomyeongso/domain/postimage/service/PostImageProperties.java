package org.example.moomyeongso.domain.postimage.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.post-images")
public class PostImageProperties {

    private int maxImagesPerPost = 5;
    private long maxFileSizeBytes = 10 * 1024 * 1024;
    private int thumbnailSize = 512;
    private long temporaryRetentionHours = 24;
    private long deletedRetentionDays = 7;
}

