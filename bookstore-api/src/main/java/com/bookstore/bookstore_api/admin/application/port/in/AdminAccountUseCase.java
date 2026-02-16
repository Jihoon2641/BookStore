package com.bookstore.bookstore_api.admin.application.port.in;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.AdminLoginResponse;
import com.bookstore.bookstore_api.admin.domain.model.Admin;

public interface AdminAccountUseCase {

    Admin signUp(String adminId, String password);

    AdminLoginResponse login(String adminId, String password);
}
