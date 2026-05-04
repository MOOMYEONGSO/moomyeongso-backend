package org.example.moomyeongso.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        String nickname,
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "비밀번호는 숫자 4자리로 입력해주세요.")
        String password
) {}
