package com.bookstore.bookstore_api.user.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.user.domain.entity.UserEntity;
import com.bookstore.bookstore_api.user.domain.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccountAdapter implements UserAccountRepository {

    private final UserAccountJpaRepository userAccountJpaRepository;
    private final UserConverter userMapper;

    @Override
    public User save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);

        if (userEntity == null) {
            throw new RuntimeException("사용자 저장에 실패하였습니다.");
        }
    
        UserEntity savedEntity = userAccountJpaRepository.save(userEntity);
    
        return userMapper.toModel(savedEntity);
    }

    @Override
    public User findByEmail(String email) {
        return userAccountJpaRepository.findByEmail(email)
            .map(userMapper::toModel)
            .orElse(null);
    }
}
