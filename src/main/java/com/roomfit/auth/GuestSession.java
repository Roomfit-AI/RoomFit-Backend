package com.roomfit.auth;

/**
 * 발급 직후에만 사용하는 값 객체 — guestId와 그 guestId를 서명한 token 쌍.
 * AuthController가 이 그대로를 응답 DTO로 노출한다.
 */
public record GuestSession(String guestId, String token) {
}
