package org.example.moomyeongso.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.moomyeongso.domain.user.entity.VisitMotive;

public record SignupRequestDto(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식으로 입력해주세요.")
        String email,
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        String nickname,
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(regexp = "\\d{4}", message = "비밀번호는 숫자 4자리로 입력해주세요.")
        String password,
        @NotNull(message = "방문동기를 선택해주세요.")
        VisitMotive visitMotive
) {}
