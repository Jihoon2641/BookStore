package com.bookstore.bookstore_api.user.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.bookstore_api.user.adapter.in.dto.request.LogInDto;
import com.bookstore.bookstore_api.user.adapter.in.dto.request.SignUpDto;
import com.bookstore.bookstore_api.user.adapter.in.dto.response.UserAccountResponseDto;
import com.bookstore.bookstore_api.user.application.port.in.LogInCommand;
import com.bookstore.bookstore_api.user.application.port.in.SignUpCommand;
import com.bookstore.bookstore_api.user.application.port.in.UserAccountUseCase;
import com.bookstore.bookstore_api.user.domain.model.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserAccountController {

    private final UserAccountUseCase userAccountUseCase;

    @PostMapping("/signup")
    public ResponseEntity<Object> signUp(@Valid @RequestBody SignUpDto signUpDto) {
        SignUpCommand command = new SignUpCommand(
            signUpDto.name(),
            signUpDto.email(),
            signUpDto.password()
        );
        
        User user = userAccountUseCase.signUp(command);

        UserAccountResponseDto responseDto = new UserAccountResponseDto(
            user.getName(),
            user.getEmail()
        );

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LogInDto loginDto) {
        LogInCommand command = new LogInCommand(
            loginDto.email(),
            loginDto.password()
        );
        
        User user = userAccountUseCase.login(command);

        UserAccountResponseDto responseDto = new UserAccountResponseDto(
            user.getName(),
            user.getEmail()
        );

        return ResponseEntity.ok(responseDto);
    }
    
}
