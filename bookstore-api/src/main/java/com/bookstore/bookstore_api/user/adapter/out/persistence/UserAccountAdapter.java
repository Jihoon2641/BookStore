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
    private final UserMapper userMapper;

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

        UserEntity userEntity = userAccountJpaRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return userMapper.toModel(userEntity);
    }
}
