package org.example.moomyeongso.domain.postimage.service;

import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Locale;

enum ImageFormat {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp");

    private final String extension;
    private final String contentType;

    ImageFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    String extension() {
        return extension;
    }

    String contentType() {
        return contentType;
    }

    static ImageFormat detect(byte[] bytes, String declaredContentType) {
        ImageFormat detected = detectBySignature(bytes);
        if (!StringUtils.hasText(declaredContentType)) {
            return detected;
        }

        String normalized = declaredContentType.toLowerCase(Locale.ROOT);
        if (detected.contentType.equals(normalized) || (detected == JPEG && "image/jpg".equals(normalized))) {
            return detected;
        }
        throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    private static ImageFormat detectBySignature(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return JPEG;
        }

        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return PNG;
        }

        if (bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50) {
            return WEBP;
        }

        throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }
}

