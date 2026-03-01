package org.example.moomyeongso.metrics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.example.moomyeongso.domain.post.entity.PostType;
import org.example.moomyeongso.domain.post.repository.PostRepository;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.example.moomyeongso.metrics.dto.TodayMetricsDto;
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

        long moomyeongsoPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.MOOMYEONGSO, PostStatus.ACTIVE, start, end);
        long moomyeongsoTotalPosts = postRepository.countByTypeAndStatus(PostType.MOOMYEONGSO, PostStatus.ACTIVE);
        long diaryPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.DIARY, PostStatus.ACTIVE, start, end);
        long diaryTotalPosts = postRepository.countByTypeAndStatus(PostType.DIARY, PostStatus.ACTIVE);
        long todayPosts = postRepository.countByTypeAndStatusAndCreatedAtBetween(
                PostType.TODAY, PostStatus.ACTIVE, start, end);
        long todayTotalPosts = postRepository.countByTypeAndStatus(PostType.TODAY, PostStatus.ACTIVE);
        long members = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.USER, start, end);
        long anonymous = userRepository.countByUserRoleAndCreatedAtBetween(UserRole.ANONYMOUS, start, end);
        long totalMembers =  userRepository.countByUserRole(UserRole.USER);
        return new TodayMetricsDto(moomyeongsoPosts,moomyeongsoTotalPosts, diaryPosts, diaryTotalPosts,
                todayPosts,todayTotalPosts, members, anonymous,totalMembers);
    }
}
