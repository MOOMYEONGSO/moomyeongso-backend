package org.example.moomyeongso.domain.postimage.service;

public record ProcessedImage(
        byte[] originalBytes,
        byte[] thumbnailBytes,
        ImageFormat format,
        int width,
        int height
) {
}

