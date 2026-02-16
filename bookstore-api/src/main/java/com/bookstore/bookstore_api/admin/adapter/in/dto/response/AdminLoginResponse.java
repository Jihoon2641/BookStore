package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import lombok.Builder;

@Builder
public record AdminLoginResponse(
        String accessToken,
        Long id,
        String adminId,
        String role) {
}
