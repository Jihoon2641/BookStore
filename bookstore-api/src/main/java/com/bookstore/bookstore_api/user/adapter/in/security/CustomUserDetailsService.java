package com.bookstore.bookstore_api.user.adapter.in.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bookstore.bookstore_api.user.application.port.out.UserAccountRepository;
import com.bookstore.bookstore_api.admin.application.port.out.RoleRepository;
import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;
import com.bookstore.bookstore_api.admin.domain.model.Roles;
import com.bookstore.bookstore_api.user.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userAccountRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        RolesEntity role = roleRepository.findById(user.getRoleId());

        Roles roles = Roles.create(role.getId(), role.getRole(), role.getDescription());

        return new CustomUserDetails(user, roles);
    }
}
