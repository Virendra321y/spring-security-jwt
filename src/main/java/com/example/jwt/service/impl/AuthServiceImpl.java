package com.example.jwt.service.impl;

import com.example.jwt.dto.RefreshTokenRequest;
import com.example.jwt.dto.SignInRequest;
import com.example.jwt.dto.SignUpRequest;
import com.example.jwt.dto.TokenResponse;
import com.example.jwt.dto.UserDto;
import com.example.jwt.entity.RefreshToken;
import com.example.jwt.entity.User;
import com.example.jwt.repository.UserRepository;
import com.example.jwt.service.AuthService;
import com.example.jwt.service.JwtService;
import com.example.jwt.service.RefreshTokenService;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           AuthenticationManager authenticationManager,
                           ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.modelMapper = modelMapper;
    }

    @Override
    public UserDto signup(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }

        User user = User.builder()
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .address(signUpRequest.getAddress())
                .salary(signUpRequest.getSalary())
                .role("ROLE_USER")
                .build();

        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public TokenResponse signin(SignInRequest signInRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signInRequest.getEmail(),
                        signInRequest.getPassword()
                )
        );

        User user = userRepository.findByEmail(signInRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + signInRequest.getEmail()));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenService.findByToken(refreshTokenRequest.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    return TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .userId(user.getId())
                            .email(user.getEmail())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
