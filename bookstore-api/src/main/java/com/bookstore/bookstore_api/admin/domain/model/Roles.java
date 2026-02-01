package com.bookstore.bookstore_api.admin.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.bookstore.bookstore_api.admin.domain.entity.AdminRole;

@Schema(description = "역할")
@Getter
@AllArgsConstructor
public class Roles {

    @Schema(description = "역할 이름")
    private AdminRole role;

    @Schema(description = "역할 설명")
    private String description;

    /**
     * 신규 역할 생성
     * 
     * @param role        역할
     * @param description 역할 설명
     * @return 신규 역할 정보
     */
    public static Roles create(AdminRole role, String description) {
        validateRoleName(role);
        validateDescription(description);
        return new Roles(role, description);
    }

    /* =============== 검증 메서드 =============== */

    /**
     * 역할 이름 유효성 검사
     * 
     * @param role 역할
     */
    private static void validateRoleName(AdminRole role) {
        if (role == null) {
            throw new IllegalArgumentException("역할 이름은 필수입니다.");
        }
    }

    /**
     * 역할 설명 유효성 검사
     * 
     * @param description 역할 설명
     */
    private static void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("역할 설명은 필수입니다.");
        }
    }

}
