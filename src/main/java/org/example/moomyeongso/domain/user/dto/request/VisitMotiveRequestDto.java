package org.example.moomyeongso.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.moomyeongso.domain.user.entity.VisitMotive;

public record VisitMotiveRequestDto(
        @NotNull(message = "방문동기를 선택해주세요.")
        VisitMotive visitMotive
) {}
