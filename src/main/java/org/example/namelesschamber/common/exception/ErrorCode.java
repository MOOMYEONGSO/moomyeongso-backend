package org.example.namelesschamber.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, 1000,"잘못된 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,1001, "서버 내부 오류가 발생했습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND,1002, "해당 ID의 게시글을 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST,1004, "이미 사용 중인 닉네임입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,1005, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED,1006, "비밀번호가 올바르지 않습니다."),
    CONTENT_TOO_SHORT_FOR_SHORT(HttpStatus.BAD_REQUEST,1007, "조금 더 이야기해주세요. 30자 이상 적어야 흘려보낼 수 있어요."),
    CONTENT_TOO_SHORT_FOR_LONG(HttpStatus.BAD_REQUEST,1008, "조금 더 이야기해주세요. 100자 이상 적어야 흘려보낼 수 있어요."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,1009, "이미 사용 중인 이메일입니다."),
    USER_NOT_ACTIVE(HttpStatus.BAD_REQUEST,1010,"활성화된 계정이 아닙니다."),
    NOT_ENOUGH_COIN(HttpStatus.BAD_REQUEST,1011,"가지고 있는 코인이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,1012, "토큰이 유효하지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 1013, "지원하지 않는 HTTP 메서드입니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, 1014, "잘못된 요청 형식입니다."),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, 1015, "이미 가입된 사용자입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,1016, "만료된 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1017, "인증이 필요합니다."),
    NICKNAME_NOT_FOUND(HttpStatus.CONFLICT,1018,"접근을 위해 닉네임 설정이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 1019, "접근 권한이 없습니다."),
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, 1020, "오늘의 주제를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
