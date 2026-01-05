package com.bookstore.bookstore_api.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookstore.bookstore_api.user.domain.entity.UserEntity;
import java.util.Optional;

public interface UserAccountJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);
}
