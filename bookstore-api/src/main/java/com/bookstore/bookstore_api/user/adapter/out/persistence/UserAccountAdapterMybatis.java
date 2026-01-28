package com.bookstore.bookstore_api.user.adapter.out.persistence;

import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.user.domain.entity.UserEntity;
import com.bookstore.bookstore_api.user.domain.model.User;

import lombok.RequiredArgsConstructor;

import java.util.Optional;


@RequiredArgsConstructor
public class UserAccountAdapterMybatis implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);

        if (userEntity == null) {
            throw new RuntimeException("사용자 저장에 실패하였습니다.");
        }

        int result = userAccountMapper.save(userEntity);

        if (result == 0) {
            throw new RuntimeException("사용자 저장에 실패하였습니다.");
        }

        return userMapper.toModel(userEntity);  
    }

    @Override
    public User findByEmail(String email) {
        Optional<UserEntity> userEntity = userAccountMapper.findByEmail(email);

        if (userEntity.isPresent()) {
            return userMapper.toModel(userEntity.get());
        } else {
            throw new RuntimeException("사용자 조회에 실패하였습니다.");
        }

    }
}
