package com.bookstore.bookstore_api.user.application.port.in;

import com.bookstore.bookstore_api.user.adapter.in.dto.response.UserLoginResponse;
import com.bookstore.bookstore_api.user.domain.model.User;

public interface UserAccountUseCase {

    User signUp(SignUpCommand command);

    UserLoginResponse login(LogInCommand command);

    void singOut(String email);

}
