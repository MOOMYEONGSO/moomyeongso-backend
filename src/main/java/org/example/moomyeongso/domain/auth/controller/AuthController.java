package org.example.moomyeongso.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.response.ApiResponse;
import org.example.moomyeongso.domain.auth.core.SecurityUtils;
import org.example.moomyeongso.domain.auth.dto.request.LoginRequestDto;
import org.example.moomyeongso.domain.auth.dto.request.ReissueRequestDto;
import org.example.moomyeongso.domain.auth.dto.request.SignupRequestDto;
import org.example.moomyeongso.domain.auth.dto.response.LoginResponseDto;
import org.example.moomyeongso.domain.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "회원가입 및 로그인 API")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "닉네임과 숫자 4자리 비밀번호를 입력받아 회원가입을 수행합니다. 익명 사용자 토큰이 있으면 해당 계정을 회원으로 전환합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request) {

        String subject = SecurityUtils.getCurrentSubjectOrEmpty().orElse(null);
        LoginResponseDto response = authService.signup(request, subject);

        return ApiResponse.success(HttpStatus.CREATED, response);
    }

    @Operation(summary = "로그인", description = "닉네임과 숫자 4자리 비밀번호를 입력받아 로그인합니다. 성공 시 회원 정보와 토큰을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        String anonymousSubject = SecurityUtils.getCurrentSubjectOrEmpty().orElse(null);
        LoginResponseDto response = authService.login(request, anonymousSubject);
        return ApiResponse.success(HttpStatus.OK, response);
    }

    @Operation(
            summary = "익명 로그인",
            description = "익명 사용자로 로그인합니다. 회원이 아닌 경우에도 JWT 토큰을 발급받아 글쓰기 등 기능을 사용할 수 있습니다."
    )
    @PostMapping("/anonymous")
    public ResponseEntity<ApiResponse<LoginResponseDto>> anonymousLogin() {
        LoginResponseDto response = authService.loginAsAnonymous();
        return ApiResponse.success(HttpStatus.OK, response);
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다. " +
                    "Access Token은 만료 시까지 유효하지만, Refresh Token이 삭제되므로 재발급이 불가능해집니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        String userId = SecurityUtils.getCurrentSubject();
        authService.logout(userId);
        return ApiResponse.success(HttpStatus.OK);
    }

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 Access Token과 Refresh Token을 사용해 새로운 Access Token과 Refresh Token을 발급합니다."
    )
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponseDto>> reissue(
            @Valid @RequestBody ReissueRequestDto request) {

        LoginResponseDto response = authService.reissueTokens(request);
        return ApiResponse.success(HttpStatus.OK, response);
    }
}
