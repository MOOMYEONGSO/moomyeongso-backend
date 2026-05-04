package org.example.moomyeongso.domain.user.dto.response;

import lombok.Builder;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.VisitMotive;

import java.time.LocalDateTime;

@Builder
public record UserInfoResponseDto(
        String userId,
        String nickname,
        String role,
        int coin,
        LocalDateTime createdAt,
        int currentStreak,
        VisitMotive visitMotive
) {
    public static UserInfoResponseDto from(User user) {
        return UserInfoResponseDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getUserRole().name())
                .coin(user.getCoin())
                .createdAt(user.getCreatedAt())
                .currentStreak(user.getStreak().getCurrent())
                .visitMotive(user.getVisitMotive())
                .build();
    }
}
