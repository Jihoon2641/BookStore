package com.bookstore.bookstore_api.user.adapter.in.dto.response;

import lombok.Builder;

@Builder
public record UserLoginResponse(
        String accessToken,
        Long id,
        String email,
        String name,
        String role) {
}
