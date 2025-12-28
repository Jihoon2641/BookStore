package com.bookstore.bookstore_api.user.application.service;

import org.springframework.stereotype.Service;

import com.bookstore.bookstore_api.user.application.port.in.UserAccountUseCase;
import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.user.domain.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService implements UserAccountUseCase {

    private final UserAccountRepository userAccountRepository;

    @Override
    public User signUp(String name, String email, String password) {
        User user = userAccountRepository.findByEmail(email);

        if (user != null) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User savedUser = userAccountRepository.save(
            User.create(name, email, password)
        );
        return savedUser;
    }

    @Override
    public User login(String email, String password) {
        User user = userAccountRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("존재하지 않는 이메일입니다.");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    @Override
    public void singOut(String email) {
        
    }



}
