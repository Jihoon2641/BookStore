package com.bookstore.bookstore_api.user.application.port.out;

import com.bookstore.bookstore_api.user.domain.model.User;

public interface UserAccountRepository {
    
    User save(User user);
    User findByEmail(String email);
    
}
