package com.bookstore.bookstore_api.admin.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Admin {

    private Long id;
    private String adminId;
    private String password;
    private Long roleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Admin create(String adminId, String password, Long roleId) {
        validateAdminId(adminId);
        validatePassword(password);
        if (roleId == null) {
            throw new IllegalArgumentException("관리자 권한 ID는 필수입니다.");
        }

        return new Admin(
                null,
                adminId,
                password,
                roleId,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private static void validateAdminId(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("관리자 아이디는 필수입니다.");
        }

        if (adminId.length() < 4 || adminId.length() > 30) {
            throw new IllegalArgumentException("관리자 아이디는 4자 이상 30자 이하여야 합니다.");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("관리자 비밀번호는 필수입니다.");
        }

        if (password.length() < 8 || password.length() > 15) {
            throw new IllegalArgumentException("관리자 비밀번호는 8자 이상 15자 이하여야 합니다.");
        }
    }
}
