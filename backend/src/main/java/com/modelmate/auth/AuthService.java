package com.modelmate.auth;

import com.modelmate.auth.dto.AuthDtos.AuthResponse;
import com.modelmate.auth.dto.AuthDtos.LoginRequest;
import com.modelmate.auth.dto.AuthDtos.RegisterRequest;
import com.modelmate.common.exception.ConflictException;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.security.AuthUser;
import com.modelmate.security.JwtService;
import com.modelmate.user.Role;
import com.modelmate.user.User;
import com.modelmate.user.UserDto;
import com.modelmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with that email already exists");
        }
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        users.save(user);
        return new AuthResponse(jwtService.generateAccessToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new AuthResponse(jwtService.generateAccessToken(user), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(AuthUser principal) {
        return users.findById(principal.id())
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
