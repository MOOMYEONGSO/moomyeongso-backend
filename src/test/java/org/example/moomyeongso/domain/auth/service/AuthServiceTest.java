package org.example.moomyeongso.domain.auth.service;

import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.common.util.EncoderUtils;
import org.example.moomyeongso.domain.auth.dto.request.LoginRequestDto;
import org.example.moomyeongso.domain.auth.dto.request.SignupRequestDto;
import org.example.moomyeongso.domain.auth.dto.response.LoginResponseDto;
import org.example.moomyeongso.domain.auth.jwt.JwtTokenProvider;
import org.example.moomyeongso.domain.auth.repository.RefreshTokenRepository;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.example.moomyeongso.domain.user.service.StreakService;
import org.example.moomyeongso.domain.visithistory.service.VisitHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EncoderUtils encoderUtils;

    @Mock
    private VisitHistoryService visitHistoryService;

    @Mock
    private StreakService streakService;

    @Mock
    private MigrationService migrationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshValidityInMs", 1_209_600_000L);
    }

    @Test
    void loginFindsUserByEmail() {
        User user = User.builder()
                .id("user-id")
                .email("user@example.com")
                .nickname("nick")
                .passwordHash("encoded-password")
                .userRole(UserRole.USER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(encoderUtils.matches("1234", "encoded-password")).thenReturn(true);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken("user-id", "USER")).thenReturn("access-token");

        LoginResponseDto response = authService.login(new LoginRequestDto("user@example.com", "1234"), null);

        assertThat(response.userId()).isEqualTo("user-id");
        assertThat(response.nickname()).isEqualTo("nick");
        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).findByEmail("user@example.com");
        verify(userRepository, never()).findByNickname(any());
    }

    @Test
    void signupRejectsDuplicateEmailBeforeCheckingNickname() {
        SignupRequestDto request = new SignupRequestDto("user@example.com", "nick", "1234");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request, null))
                .isInstanceOfSatisfying(CustomException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));

        verify(userRepository, never()).existsByNickname(eq("nick"));
        verify(userRepository, never()).save(any());
    }
}
