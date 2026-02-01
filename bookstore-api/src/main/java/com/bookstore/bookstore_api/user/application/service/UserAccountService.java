package com.bookstore.bookstore_api.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.bookstore.bookstore_api.user.adapter.in.dto.response.UserLoginResponse;
import com.bookstore.bookstore_api.user.application.port.in.LogInCommand;
import com.bookstore.bookstore_api.user.application.port.in.SignUpCommand;
import com.bookstore.bookstore_api.user.application.port.in.UserAccountUseCase;
import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.user.domain.model.User;
import com.bookstore.bookstore_api.admin.application.port.out.RoleRepository;
import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;
import com.bookstore.bookstore_api.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountService implements UserAccountUseCase {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public User signUp(SignUpCommand command) {
        User user = userAccountRepository.findByEmail(command.getEmail());

        if (user != null) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User savedUser = userAccountRepository.save(
                User.create(command.getName(), command.getEmail(), command.getPassword(), 1L, LocalDateTime.now(),
                        LocalDateTime.now()));
        return savedUser;
    }

    @Override
    @Transactional
    public UserLoginResponse login(LogInCommand command) {
        User user = userAccountRepository.findByEmail(command.getEmail());

        if (user == null) {
            throw new RuntimeException("존재하지 않는 이메일입니다.");
        }

        if (!user.getPassword().equals(command.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        RolesEntity role = roleRepository.findById(user.getRoleId());

        if (role == null) {
            throw new RuntimeException("사용자 권한 정보를 찾을 수 없습니다.");
        }

        String token = jwtUtil.createToken(user.getId(), user.getEmail(), role.getRole().name());

        return UserLoginResponse.builder()
                .accessToken(token)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(role.getRole().name())
                .build();
    }

    @Override
    public void singOut(String email) {

    }

}
