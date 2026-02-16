package com.bookstore.bookstore_api.admin.adapter.out;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.admin.application.port.out.AdminRepository;
import com.bookstore.bookstore_api.admin.domain.entity.AdminEntity;
import com.bookstore.bookstore_api.admin.domain.model.Admin;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAdapter implements AdminRepository {

    private final AdminMapper adminMapper;

    @Override
    public Admin save(Admin admin) {
        AdminEntity adminEntity = AdminEntity.builder()
                .id(admin.getId())
                .adminId(admin.getAdminId())
                .password(admin.getPassword())
                .roleId(admin.getRoleId())
                .build();

        int result = adminMapper.save(adminEntity);
        if (result == 0) {
            throw new RuntimeException("관리자 저장에 실패하였습니다.");
        }

        return toModel(adminEntity);
    }

    @Override
    public Admin findByAdminId(String adminId) {
        Optional<AdminEntity> adminEntity = adminMapper.findByAdminId(adminId);
        return adminEntity.map(this::toModel).orElse(null);
    }

    private Admin toModel(AdminEntity entity) {
        return new Admin(
                entity.getId(),
                entity.getAdminId(),
                entity.getPassword(),
                entity.getRoleId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
