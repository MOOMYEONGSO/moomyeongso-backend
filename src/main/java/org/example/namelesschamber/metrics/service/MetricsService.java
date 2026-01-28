package org.example.namelesschamber.metrics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.namelesschamber.domain.post.entity.PostStatus;
import org.example.namelesschamber.domain.post.entity.PostType;
import org.example.namelesschamber.domain.post.repository.PostRepository;
import org.example.namelesschamber.domain.user.entity.UserRole;
import org.example.namelesschamber.domain.user.repository.UserRepository;
import org.example.namelesschamber.metrics.dto.TodayMetricsDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;


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

        long shortPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.SHORT, PostStatus.ACTIVE, start, end);
        long shortTotalPosts = postRepository.countByTypeAndStatus(PostType.SHORT, PostStatus.ACTIVE);
        long longPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.LONG, PostStatus.ACTIVE, start, end);
        long longTotalPosts = postRepository.countByTypeAndStatus(PostType.LONG, PostStatus.ACTIVE);
        long todayPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.TODAY, PostStatus.ACTIVE, start, end);
        long todayTotalPosts = postRepository.countByTypeAndStatus(PostType.TODAY, PostStatus.ACTIVE);
        long members = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.USER, start, end);
        long anonymous = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.ANONYMOUS, start, end);
        long totalMembers =  userRepository.countByUserRole(UserRole.USER);
        return new TodayMetricsDto(shortPosts,shortTotalPosts, longPosts, longTotalPosts,
                todayPosts,todayTotalPosts, members, anonymous,totalMembers);
    }
}
