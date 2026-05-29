package org.example.moomyeongso.domain.postimage.storage;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.config.aws.S3StorageProperties;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    @Override
    public StoredImageObject upload(byte[] bytes, String key, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(resolveBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .cacheControl("public, max-age=31536000")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return new StoredImageObject(key, resolveUrl(key));
        } catch (RuntimeException ex) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(resolveBucket())
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    private String resolveBucket() {
        String bucket = properties.getS3().getBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return bucket;
    }

    private String resolveUrl(String key) {
        String baseUrl = properties.getS3().getPublicBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl.replaceAll("/+$", "") + "/" + key;
        }
        return "https://" + resolveBucket() + ".s3." + properties.getRegion() + ".amazonaws.com/" + key;
    }
}

