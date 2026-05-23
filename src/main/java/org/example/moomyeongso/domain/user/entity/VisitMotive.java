package org.example.moomyeongso.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisitMotive {
    COMFORT("위로 받고 싶어요"),
    PROBLEM("문제를 해결하고 싶어요"),
    CURIOUS("다른 사람들이 궁금해요"),
    UNKNOWN("아직 잘 모르겠어요");

    private final String label;
}
