package com.bookstore.bookstore_api.user.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.bookstore_api.user.adapter.in.dto.LogInDto;
import com.bookstore.bookstore_api.user.adapter.in.dto.SignUpDto;
import com.bookstore.bookstore_api.user.application.service.UserAccountService;
import com.bookstore.bookstore_api.user.domain.model.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @PostMapping("/signup")
    public ResponseEntity<Object> signUp(SignUpDto signUpDto){
        User user = userAccountService.signUp(signUpDto.name(), signUpDto.email(), signUpDto.password());

        return ResponseEntity.ok(String.format("%s님 환영합니다.", user.getName()));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(LogInDto loginDto){
        User user = userAccountService.login(loginDto.email(), loginDto.password());

        return ResponseEntity.ok(String.format("%s님 환영합니다.", user.getName()));
    }
    
}
