package io.hhplus.tdd.common.response;

public record ErrorResponse(
        String code,
        String message
) {
}
