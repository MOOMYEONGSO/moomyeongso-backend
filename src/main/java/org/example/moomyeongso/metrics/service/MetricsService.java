package org.example.moomyeongso.metrics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.example.moomyeongso.metrics.dto.TodayMetricsDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public TodayMetricsDto getTodayMetrics() {
        LocalDate todayKst = LocalDate.now(KST);
        Instant start = todayKst.atStartOfDay(KST).toInstant();
        Instant end   = todayKst.plusDays(1).atStartOfDay(KST).toInstant();
        List<String> adminUserIds = getAdminUserIds();

        long textPosts = countPostsByTypeBetweenExcludingAdmins(PostType.TEXT, start, end, adminUserIds);
        long textTotalPosts = countPostsByTypeExcludingAdmins(PostType.TEXT, adminUserIds);
        long imagePosts = countPostsByTypeBetweenExcludingAdmins(PostType.IMAGE, start, end, adminUserIds);
        long imageTotalPosts = countPostsByTypeExcludingAdmins(PostType.IMAGE, adminUserIds);
        long members = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.USER, start, end);
        long anonymous = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.ANONYMOUS, start, end);
        long totalMembers =  userRepository.countByUserRole(UserRole.USER);
        return new TodayMetricsDto(textPosts, textTotalPosts, imagePosts, imageTotalPosts,
                members, anonymous,totalMembers);
    }

    private List<String> getAdminUserIds() {
        return userRepository.findAllByUserRole(UserRole.ADMIN).stream()
                .map(User::getId)
                .toList();
    }

    private long countPostsByTypeBetweenExcludingAdmins(
            PostType type,
            Instant start,
            Instant end,
            List<String> adminUserIds
    ) {
        if (adminUserIds.isEmpty()) {
            return postRepository.countByTypeAndStatusAndCreatedAtBetween(type, PostStatus.ACTIVE, start, end);
        }
        return postRepository.countByTypeAndStatusAndUserIdNotInAndCreatedAtBetween(
                type,
                PostStatus.ACTIVE,
                adminUserIds,
                start,
                end
        );
    }

    private long countPostsByTypeExcludingAdmins(PostType type, List<String> adminUserIds) {
        if (adminUserIds.isEmpty()) {
            return postRepository.countByTypeAndStatus(type, PostStatus.ACTIVE);
        }
        return postRepository.countByTypeAndStatusAndUserIdNotIn(type, PostStatus.ACTIVE, adminUserIds);
    }
}
