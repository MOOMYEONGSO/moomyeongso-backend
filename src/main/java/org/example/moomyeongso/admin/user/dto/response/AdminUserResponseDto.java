package org.example.moomyeongso.admin.user.dto.response;

import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.example.moomyeongso.domain.user.entity.UserStatus;
import org.example.moomyeongso.domain.user.entity.VisitMotive;

import java.time.LocalDateTime;

public record AdminUserResponseDto(
        String id,
        String nickname,
        UserRole userRole,
        UserStatus status,
        LocalDateTime createdAt,
        VisitMotive visitMotive
) {
    public static AdminUserResponseDto from(User user) {
        return new AdminUserResponseDto(
                user.getId(),
                user.getNickname(),
                user.getUserRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getVisitMotive()
        );
    }
}
