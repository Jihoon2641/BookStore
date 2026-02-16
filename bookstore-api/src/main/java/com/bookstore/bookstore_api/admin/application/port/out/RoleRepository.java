package com.bookstore.bookstore_api.admin.application.port.out;

import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;

public interface RoleRepository {
    RolesEntity findById(Long id);

    RolesEntity findByRole(String role);
}
