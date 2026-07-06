package com.example.jwt.service;

import com.example.jwt.dto.RefreshTokenRequest;
import com.example.jwt.dto.SignInRequest;
import com.example.jwt.dto.SignUpRequest;
import com.example.jwt.dto.TokenResponse;
import com.example.jwt.dto.UserDto;

public interface AuthService {
    UserDto signup(SignUpRequest signUpRequest);
    TokenResponse signin(SignInRequest signInRequest);
    TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}
