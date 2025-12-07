package io.hhplus.tdd.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // user
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),

    // point
    POINT_BALANCE_OVER(HttpStatus.CONFLICT, "포인트 잔액이 최대를 초과헀습니다."),
    POINT_BALANCE_NEGATIVE(HttpStatus.CONFLICT, "포인트 잔액은 0보다 커야 합니다."),
    POINT_LESS_THAN_100(HttpStatus.BAD_REQUEST, "포인트 잔액은 최소 100원을 사용해야합니다."),
        ;

    private final HttpStatus httpStatus;
    private final String errMsg;
}
