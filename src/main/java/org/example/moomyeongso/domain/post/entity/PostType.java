package org.example.moomyeongso.domain.post.entity;

import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;

public enum PostType {
    MOOMYEONGSO(30, ErrorCode.CONTENT_TOO_SHORT),
    DIARY(30, ErrorCode.CONTENT_TOO_SHORT),
    TODAY(30, ErrorCode.CONTENT_TOO_SHORT);

    private final int minLength;
    private final ErrorCode errorCodeForTooShort;

    PostType(int minLength, ErrorCode errorCode) {
        this.minLength = minLength;
        this.errorCodeForTooShort = errorCode;
    }

    public void validateContentLength(String content) {
        if (content.length() < this.minLength) {
            throw new CustomException(this.errorCodeForTooShort);
        }
    }
}