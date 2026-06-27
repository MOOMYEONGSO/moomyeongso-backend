package org.example.moomyeongso.metrics.dto;

public record TodayMetricsDto(
        long textPosts,
        long textTotalPosts,
        long imagePosts,
        long imageTotalPosts,
        long members,
        long anonymous,
        long totalMembers
) {}
