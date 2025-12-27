package com.bookstore.bookstore_api.domain.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "사용자 정보")
@Getter
@AllArgsConstructor
public class User {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Schema(description = "사용자 ID")
    private Long id;
    @Schema(description = "사용자 이름")
    private String name;
    @Schema(description = "사용자 이메일")
    private String email;
    @Schema(description = "사용자 비밀번호")
    private String password;

    /**
     * 신규 사용자 생성
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 신규 사용자 정보
     */
    public static User create(String name, String email, String password) {
        validateName(name);
        validateEmail(email);
        validatePassword(password);

        return new User(null, name, email, password);
    }

    /**
     * 사용자 비밀번호 변경
     * @param password 사용자 비밀번호
     */
    public void changePassword(String CurrentPassword, String newPassword) {
        validatePassword(CurrentPassword);
        validatePassword(newPassword);

        if (!CurrentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("현재 비밀번호와 새 비밀번호가 일치하지 않습니다.");
        }
        
        this.password = newPassword;
    }

    /* =============== 검증 메서드 =============== */
    
    /**
     * 사용자 이름 유효성 검사
     * @param name 사용자 이름
     */
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("사용자 이름은 필수입니다.");
        }

        if (name.length() < 2 || name.length() > 15) {
            throw new IllegalArgumentException("사용자 이름은 2자 이상 15자 이하여야 합니다.");
        }
    }

    /** 
     * 사용자 이메일 유효성 검사
     * @param email 사용자 이메일
     */
    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("사용자 이메일은 필수입니다.");
        }

        if (!email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 사용자 비밀번호 유효성 검사
     * @param password 사용자 비밀번호
     */
    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("사용자 비밀번호는 필수입니다.");
        }

        if (password.length() < 8 && password.length() > 15) {
            throw new IllegalArgumentException("사용자 비밀번호는 8자 이상 15자 이하여야 합니다.");
        }
    
        if (!password.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("사용자 비밀번호는 영문자와 숫자, 특수문자를 포함해야 합니다.");
        }
        
    }
}
