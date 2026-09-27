package com.floweapp.flowe_api.auth.service;

import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterResponseDto;
import com.floweapp.flowe_api.auth.exception.EmailAlreadyExistsException;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String email = request.email();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .createdAt(OffsetDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        return toRegisterResponse(savedUser);
    }

    private RegisterResponseDto toRegisterResponse(User user) {
        return new RegisterResponseDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }
}
