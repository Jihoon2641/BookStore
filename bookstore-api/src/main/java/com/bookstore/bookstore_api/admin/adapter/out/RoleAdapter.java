package com.bookstore.bookstore_api.admin.adapter.out;

import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.admin.application.port.out.RoleRepository;
import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleAdapter implements RoleRepository {

    private final RoleMapper roleMapper;

    @Override
    public RolesEntity findById(Long id) {
        return roleMapper.findById(id);
    }

}
