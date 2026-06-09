package com.cursed.auth.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.dtos.RegisterDto;
import com.cursed.auth.dtos.VerifyOTPDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.entities.User;

public interface UserService {
    BaseResponseDTO<String> uploadUserProfile(String email, MultipartFile profileImage);

    BaseResponseDTO<RegisterResponseDTO> register(RegisterDto request);

    BaseResponseDTO<LoginResponseDTO> verifyOtp(VerifyOTPDto request);

    BaseResponseDTO<String> resendOtp(String email);

    BaseResponseDTO<UserResponseDTO> getUserByEmail(String email);

    BaseResponseDTO<LoginResponseDTO> login(String email, String password);

    BaseResponseDTO<List<UserResponseDTO>> findAllUsers();

    /**
     * Validates credentials (BCrypt + verified + active) and returns the authenticated
     * user. Shared by the plain JSON login and the OAuth authorization-code login path.
     * On failure {@code user()} is null and {@code errorMessage()} explains why.
     */
    AuthResult authenticate(String email, String password);

    record AuthResult(User user, String errorMessage) {
    }
}
