package com.bookstore.bookstore_api.user.application.port.in;

import com.bookstore.bookstore_api.util.validate.SelfValidating;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignUpCommand extends SelfValidating<SignUpCommand> {

    @NotBlank(message = "사용자 이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 15, message = "사용자 이름은 2자 이상 15자 이하여야 합니다.")
    private final String name;

    @NotBlank(message = "사용자 이메일은 필수 입력 항목입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private final String email;

    @NotBlank(message = "사용자 비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, max = 15, message = "사용자 비밀번호는 8자 이상 15자 이하여야 합니다.")
    private final String password;

    public SignUpCommand(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        validateSelf();
    }
}

