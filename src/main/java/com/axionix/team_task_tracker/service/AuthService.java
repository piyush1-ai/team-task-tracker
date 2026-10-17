package com.axionix.team_task_tracker.service;

import com.axionix.team_task_tracker.dto.LoginRequest;
import com.axionix.team_task_tracker.dto.LoginResponse;
import com.axionix.team_task_tracker.dto.RegisterRequest;
import com.axionix.team_task_tracker.dto.UserResponse;
import com.axionix.team_task_tracker.entity.User;
import com.axionix.team_task_tracker.exception.ApiException;
import com.axionix.team_task_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse register(RegisterRequest req){
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)){
            throw new ApiException(HttpStatus.CONFLICT,"EMAIL_ALREADY_EXISTS","Email already registered");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail());
    }

    public LoginResponse login(LoginRequest req){
        User user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .filter(u->passwordEncoder.matches(req.password(),u.getPasswordHash()))
                .orElseThrow(()->new ApiException(
                        HttpStatus.UNAUTHORIZED,"INVALID_CREDENTIALS","Invalid email or password"
                ));
        return new LoginResponse(jwtService.generateToken(user.getEmail()));
    }
}
