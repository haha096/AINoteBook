package com.example.ai_notebook.service;

import com.example.ai_notebook.entity.UserEntity;
import com.example.ai_notebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserEntity register(String username, String password, String email) {
        String encoded = encoder.encode(password);
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(encoded)
                .email(email)
                .build();
        return userRepository.save(user);
    }

    public Optional<UserEntity> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> encoder.matches(password, u.getPassword()));
    }
}