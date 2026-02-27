package org.example.moomyeongso.domain.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.common.util.EncoderUtils;
import org.example.moomyeongso.domain.auth.dto.request.LoginRequestDto;
import org.example.moomyeongso.domain.auth.dto.request.ReissueRequestDto;
import org.example.moomyeongso.domain.auth.dto.request.SignupRequestDto;
import org.example.moomyeongso.domain.auth.dto.response.LoginResponseDto;
import org.example.moomyeongso.domain.auth.entity.RefreshToken;
import org.example.moomyeongso.domain.auth.jwt.JwtTokenProvider;
import org.example.moomyeongso.domain.auth.repository.RefreshTokenRepository;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.example.moomyeongso.domain.user.entity.UserStatus;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.example.moomyeongso.domain.user.service.StreakService;
import org.example.moomyeongso.domain.visithistory.service.VisitHistoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncoderUtils encoderUtils;
    private final VisitHistoryService visitHistoryService;
    private final StreakService streakService;
    private final MigrationService migrationService;

    @Value("${refresh.expiration}")
    private long refreshValidityInMs;

    @Transactional("mongoTransactionManager")
    public LoginResponseDto signup(SignupRequestDto request, String subject) {

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 익명 사용자 → 회원 전환
        if (subject != null) {
            User currentUser = userRepository.findById(subject)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            if (currentUser.getUserRole().isUser()) {
                throw new CustomException(ErrorCode.ALREADY_REGISTERED);
            }

            currentUser.updateToMember(
                    request.email(),
                    encoderUtils.encode(request.password())
            );

            userRepository.save(currentUser);
            visitHistoryService.recordDailyVisit(currentUser.getId());
            streakService.updateOnVisit(currentUser);

            currentUser = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            return issueTokens(currentUser);
        }

        // 신규 회원가입
        User user = User.builder()
                .email(request.email())
                .passwordHash(encoderUtils.encode(request.password()))
                .userRole(UserRole.USER)
                .build();

        userRepository.save(user);
        visitHistoryService.recordDailyVisit(user.getId());

        return issueTokens(user);
    }

    @Transactional("mongoTransactionManager")
    public LoginResponseDto login(LoginRequestDto request, String anonymousSubject) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!encoderUtils.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (user.getUserRole() != UserRole.ANONYMOUS) {
            visitHistoryService.recordDailyVisit(user.getId());
            streakService.updateOnVisit(user);

            user = userRepository.findById(user.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        }

        migrateAnonymousDataIfNeeded(anonymousSubject, user.getId());
        user = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    @Transactional("mongoTransactionManager")
    public LoginResponseDto loginAsAnonymous() {
        User user = User.builder()
                .userRole(UserRole.ANONYMOUS)
                .expiresAt(LocalDateTime.now().plusDays(7)) // 7일 TTL
                .build();

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional("mongoTransactionManager")
    public LoginResponseDto reissueTokens(ReissueRequestDto request) {
        Claims claims = jwtTokenProvider.getClaimsEvenIfExpired(request.accessToken());
        String userId = claims.getSubject();

        RefreshToken saved = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (!encoderUtils.matches(request.refreshToken(), saved.getToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        if (saved.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    @Transactional("mongoTransactionManager")
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void migrateAnonymousDataIfNeeded(String anonymousUserId, String memberUserId) {
        if (anonymousUserId == null || anonymousUserId.equals(memberUserId)) {
            return;
        }

        userRepository.findByIdAndUserRole(anonymousUserId, UserRole.ANONYMOUS)
                .ifPresent(ignored -> {
                    migrationService.migrateAnonymousData(anonymousUserId, memberUserId);
                    refreshTokenRepository.deleteByUserId(anonymousUserId);
                    userRepository.deleteById(anonymousUserId);
                });
    }

    private LoginResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUserRole().name());
        String rawRefreshToken = UUID.randomUUID().toString();
        String encodedRefreshToken = encoderUtils.encode(rawRefreshToken);

        LocalDateTime expiryDate = LocalDateTime.now().plus(Duration.ofMillis(refreshValidityInMs));

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(encodedRefreshToken)
                .expiryDate(expiryDate)
                .build());

        return LoginResponseDto.of(user, accessToken, rawRefreshToken);
    }
}
