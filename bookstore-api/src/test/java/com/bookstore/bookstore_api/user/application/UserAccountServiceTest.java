package com.bookstore.bookstore_api.user.application;

import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import com.bookstore.bookstore_api.user.application.port.in.LogInCommand;
import com.bookstore.bookstore_api.user.application.port.in.SignUpCommand;
import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.user.application.service.UserAccountService;
import com.bookstore.bookstore_api.user.domain.model.User;

@ExtendWith(MockitoExtension.class)
public class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserAccountService userAccountService;

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        SignUpCommand command = new SignUpCommand("test", "test@example.com", "Password1!");
        when(userAccountRepository.findByEmail(anyString())).thenReturn(null);
        when(userAccountRepository.save(any())).thenReturn(User.create(command.getName(), command.getEmail(), command.getPassword()));

        User result = userAccountService.signUp(command);

        assertThat(result.getName()).isEqualTo(command.getName());
        assertThat(result.getEmail()).isEqualTo(command.getEmail());
        assertThat(result.getPassword()).isEqualTo(command.getPassword());

        verify(userAccountRepository, times(1)).findByEmail(anyString());
        verify(userAccountRepository, times(1)).save(any());

    }

    @Test
    @DisplayName("회원가입 실패 - 이메일이 이미 존재하는 경우")
    void signUp_fail_email_already_exists() {
        SignUpCommand command = new SignUpCommand("test", "test@example.com", "Password1!");

        when(userAccountRepository.findByEmail(anyString())).thenReturn(User.create(command.getName(), command.getEmail(), command.getPassword()));

        assertThatThrownBy(() -> userAccountService.signUp(command))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("이미 존재하는 이메일입니다.");

    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        LogInCommand command = new LogInCommand("test@example.com", "Password1!");
        when(userAccountRepository.findByEmail(anyString())).thenReturn(User.create("test", command.getEmail(), command.getPassword()));

        User result = userAccountService.login(command);

        assertThat(result.getEmail()).isEqualTo(command.getEmail());
        assertThat(result.getPassword()).isEqualTo(command.getPassword());

        verify(userAccountRepository, times(1)).findByEmail(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 이메일이 존재하지 않는 경우")
    void login_fail_email_not_found() {
        LogInCommand command = new LogInCommand("test@example.com", "Password1!");
        when(userAccountRepository.findByEmail(anyString())).thenReturn(null);

        assertThatThrownBy(() -> userAccountService.login(command))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("존재하지 않는 이메일입니다.");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 일치하지 않는 경우")
    void login_fail_password_not_match() {
        LogInCommand command = new LogInCommand("test@example.com", "Password1!");
        when(userAccountRepository.findByEmail(anyString())).thenReturn(User.create("test", command.getEmail(), "Password2!"));

        assertThatThrownBy(() -> userAccountService.login(command))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("비밀번호가 일치하지 않습니다.");
    }
    
}
