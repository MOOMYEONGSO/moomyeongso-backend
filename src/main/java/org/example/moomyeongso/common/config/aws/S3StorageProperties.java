package org.example.moomyeongso.common.config.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws")
public class S3StorageProperties {

    private String region;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String publicBaseUrl;
        private String originalPrefix = "posts/original";
        private String thumbnailPrefix = "posts/thumbnails";
    }
}

