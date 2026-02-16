package com.bookstore.bookstore_api.admin.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSignUpDto(

        @NotBlank(message = "관리자 아이디는 필수 입력 항목입니다.")
        @Size(min = 4, max = 30, message = "관리자 아이디는 4자 이상 30자 이하여야 합니다.")
        String adminId,

        @NotBlank(message = "관리자 비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 15, message = "관리자 비밀번호는 8자 이상 15자 이하여야 합니다.")
        String password) {
}
