package org.example.moomyeongso.domain.auth.dto.response;

import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.VisitMotive;

public record LoginResponseDto(
        String userId,
        String nickname,
        int coin,
        String accessToken,
        String refreshToken,
        String role,
        int currentStreak,
        VisitMotive visitMotive
) {
    public static LoginResponseDto of(User user, String accessToken, String refreshToken) {
        return new LoginResponseDto(
                user.getId(),
                user.getNickname(),
                user.getCoin(),
                accessToken,
                refreshToken,
                user.getUserRole().name(),
                user.getStreak().getCurrent(),
                user.getVisitMotive()
        );
    }

}
