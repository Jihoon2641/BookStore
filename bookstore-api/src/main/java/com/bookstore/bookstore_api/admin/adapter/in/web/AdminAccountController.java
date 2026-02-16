package com.bookstore.bookstore_api.admin.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.bookstore_api.admin.adapter.in.dto.request.AdminLogInDto;
import com.bookstore.bookstore_api.admin.adapter.in.dto.request.AdminSignUpDto;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.AdminAccountResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.AdminLoginResponse;
import com.bookstore.bookstore_api.admin.application.port.in.AdminAccountUseCase;
import com.bookstore.bookstore_api.admin.domain.entity.AdminRole;
import com.bookstore.bookstore_api.util.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountUseCase adminAccountUseCase;

    @PostMapping("/signup")
    public ResponseEntity<Object> signUp(@Valid @RequestBody AdminSignUpDto adminSignUpDto) {
        var admin = adminAccountUseCase.signUp(
                adminSignUpDto.adminId(),
                adminSignUpDto.password());

        AdminAccountResponse response = new AdminAccountResponse(
                admin.getAdminId(),
                AdminRole.LEVEL_1.name());

        return ResponseEntity.ok(ApiResponse.success(response, "관리자 회원가입 성공", HttpStatus.OK));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody AdminLogInDto adminLogInDto) {
        AdminLoginResponse response = adminAccountUseCase.login(
                adminLogInDto.adminId(),
                adminLogInDto.password());

        return ResponseEntity.ok(ApiResponse.success(response, "관리자 로그인 성공", HttpStatus.OK));
    }
}
