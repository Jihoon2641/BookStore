package com.bookstore.bookstore_api.admin.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.AdminLoginResponse;
import com.bookstore.bookstore_api.admin.application.port.out.RoleRepository;
import com.bookstore.bookstore_api.admin.domain.entity.AdminRole;
import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;
import com.bookstore.bookstore_api.admin.application.port.in.AdminAccountUseCase;
import com.bookstore.bookstore_api.admin.application.port.out.AdminRepository;
import com.bookstore.bookstore_api.admin.domain.model.Admin;
import com.bookstore.bookstore_api.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAccountService implements AdminAccountUseCase {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public Admin signUp(String adminId, String password) {
        Admin exists = adminRepository.findByAdminId(adminId);
        if (exists != null) {
            throw new RuntimeException("이미 존재하는 관리자 아이디입니다.");
        }

        RolesEntity level1Role = roleRepository.findByRole(AdminRole.LEVEL_1.name());
        if (level1Role == null) {
            throw new RuntimeException("LEVEL_1 역할이 존재하지 않습니다.");
        }

        return adminRepository.save(Admin.create(adminId, password, level1Role.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminLoginResponse login(String adminId, String password) {
        Admin admin = adminRepository.findByAdminId(adminId);
        if (admin == null) {
            throw new RuntimeException("존재하지 않는 관리자 아이디입니다.");
        }

        if (!admin.getPassword().equals(password)) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        RolesEntity role = roleRepository.findById(admin.getRoleId());
        if (role == null) {
            throw new RuntimeException("관리자 권한 정보를 찾을 수 없습니다.");
        }

        if (role.getRole() == AdminRole.USER) {
            throw new RuntimeException("관리자 계정이 아닙니다.");
        }

        String token = jwtUtil.createToken(admin.getId(), admin.getAdminId(), role.getRole().name());

        return AdminLoginResponse.builder()
                .accessToken(token)
                .id(admin.getId())
                .adminId(admin.getAdminId())
                .role(role.getRole().name())
                .build();
    }
}
