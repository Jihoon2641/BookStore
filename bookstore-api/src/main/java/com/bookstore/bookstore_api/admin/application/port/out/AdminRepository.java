package com.bookstore.bookstore_api.admin.application.port.out;

import com.bookstore.bookstore_api.admin.domain.model.Admin;

public interface AdminRepository {

    Admin save(Admin admin);

    Admin findByAdminId(String adminId);
}
