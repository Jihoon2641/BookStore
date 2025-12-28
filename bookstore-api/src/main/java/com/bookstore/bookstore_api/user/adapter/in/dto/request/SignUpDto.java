package com.bookstore.bookstore_api.user.adapter.in.dto.request;

import jakarta.validation.constraints.*;

public record SignUpDto(

    @NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 15, message = "사용자 이름은 2자 이상 15자 이하여야 합니다.")
    String name,

    @NotBlank(message = "사용자 이메일은 필수 입력 항목입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "사용자 비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 15, message = "사용자 비밀번호는 8자 이상 15자 이하여야 합니다.")
    String password
) {}