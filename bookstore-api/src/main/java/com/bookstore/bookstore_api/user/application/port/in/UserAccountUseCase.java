package com.bookstore.bookstore_api.user.application.port.in;

import com.bookstore.bookstore_api.user.domain.model.User;

public interface UserAccountUseCase {

    User signUp(String name, String email, String password);
    User login(String email, String password);
    void singOut(String email);

} 