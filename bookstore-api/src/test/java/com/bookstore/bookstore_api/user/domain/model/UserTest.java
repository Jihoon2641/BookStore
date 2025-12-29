package com.bookstore.bookstore_api.user.domain.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTest {

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_success() {
        String name = "test";
        String email = "test@example.com";
        String password = "Password1!";

        User user = User.create(name, email, password);

        assertNotNull(user);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("사용자 생성 실패 - 이름이 비어있는 경우")
    void createUser_fail_name_blank() {
        String name = null;
        String email = "test@example.com";
        String password = "Password1!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 이름은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 생성 실패 - 이름이 2자 미만인 경우")
    void createUser_fail_name_less_than_2() {
        String name = "a";
        String email = "test@example.com";
        String password = "Password1!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 이름은 2자 이상 15자 이하여야 합니다.");

    }

    @Test
    @DisplayName("사용자 생성 실패 - 이메일이 비어있는 경우")
    void createUser_fail_email_blank() {
        String name = "test";
        String email = null;
        String password = "Password1!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 이메일은 필수입니다.");   
    }

    @Test
    @DisplayName("사용자 생성 실패 - 이메일 형식이 올바르지 않은 경우")
    void createUser_fail_email_invalid() {
        String name = "test";
        String email = "test@example";
        String password = "Password1!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("사용자 생성 실패 - 비밀번호가 비어있는 경우")
    void createUser_fail_password_blank() {
        String name = "test";
        String email = "test@example.com";
        String password = null;

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 비밀번호는 필수입니다.");
    }

    @Test
    @DisplayName("사용자 생성 실패 - 비밀번호가 8자 미만인 경우")
    void createUser_fail_password_less_than_8() {
        String name = "test";
        String email = "test@example.com";
        String password = "Pass2!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 비밀번호는 8자 이상 15자 이하여야 합니다.");
    }
    
    @Test
    @DisplayName("사용자 생성 실패 - 비밀번호가 15자 초과인 경우")
    void createUser_fail_password_greater_than_15() {
        String name = "test";
        String email = "test@example.com";
        String password = "Password1!Password1!Password1!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 비밀번호는 8자 이상 15자 이하여야 합니다.");
    }

    @Test
    @DisplayName("사용자 생성 실패 - 비밀번호가 영문자와 숫자, 특수문자를 포함하지 않은 경우")
    void createUser_fail_password_invalid() {
        String name = "test";
        String email = "test@example.com";
        String password = "Password!";

        assertThatThrownBy(() -> User.create(name, email, password))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자 비밀번호는 영문자와 숫자, 특수문자를 포함해야 합니다.");
    }

}
