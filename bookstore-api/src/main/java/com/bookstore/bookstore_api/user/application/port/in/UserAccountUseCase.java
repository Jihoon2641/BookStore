package com.bookstore.bookstore_api.user.application.port.in;

import com.bookstore.bookstore_api.user.domain.model.User;

public interface UserAccountUseCase {

    User signUp(SignUpCommand command);
    User login(LogInCommand command);
    void singOut(String email);

}
