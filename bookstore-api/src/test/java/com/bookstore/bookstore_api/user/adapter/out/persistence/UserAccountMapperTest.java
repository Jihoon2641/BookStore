package com.bookstore.bookstore_api.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.bookstore.bookstore_api.user.domain.entity.UserEntity;

@MybatisTest
class UserAccountMapperTest {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Test
    @DisplayName("사용자 저장 후 이메일로 조회할 수 있다")
    void saveAndFindByEmail() {
        // given
        UserEntity user = UserEntity.builder()
                .name("홍길동")
                .email("hong@example.com")
                .password("password123!")
                .build();

        // when
        int result = userAccountMapper.save(user);

        // then
        assertThat(result).isEqualTo(1);
        assertThat(user.getId()).isNotNull(); // useGeneratedKeys로 ID가 설정됨

        // 이메일로 조회
        Optional<UserEntity> found = userAccountMapper.findByEmail("hong@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("홍길동");
        assertThat(found.get().getEmail()).isEqualTo("hong@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
    void findByEmailNotFound() {
        // when
        Optional<UserEntity> found = userAccountMapper.findByEmail("notfound@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("여러 사용자를 저장하고 각각 조회할 수 있다")
    void saveMultipleUsers() {
        // given
        UserEntity user1 = UserEntity.builder()
                .name("김철수")
                .email("kim@example.com")
                .password("password123!")
                .build();

        UserEntity user2 = UserEntity.builder()
                .name("이영희")
                .email("lee@example.com")
                .password("password456!")
                .build();

        // when
        userAccountMapper.save(user1);
        userAccountMapper.save(user2);

        // then
        Optional<UserEntity> foundUser1 = userAccountMapper.findByEmail("kim@example.com");
        Optional<UserEntity> foundUser2 = userAccountMapper.findByEmail("lee@example.com");

        assertThat(foundUser1).isPresent();
        assertThat(foundUser1.get().getName()).isEqualTo("김철수");

        assertThat(foundUser2).isPresent();
        assertThat(foundUser2.get().getName()).isEqualTo("이영희");

        // ID가 다른지 확인
        assertThat(foundUser1.get().getId()).isNotEqualTo(foundUser2.get().getId());
    }
}
