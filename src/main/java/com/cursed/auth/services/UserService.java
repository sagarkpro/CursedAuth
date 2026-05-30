package com.cursed.auth.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.dtos.RegisterDTO;
import com.cursed.auth.dtos.VerifyOTPDTO;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;

public interface UserService {
    BaseResponseDTO<String> uploadUserProfile(String email, MultipartFile profileImage);

    BaseResponseDTO<RegisterResponseDTO> register(RegisterDTO request);

    BaseResponseDTO<LoginResponseDTO> verifyOtp(VerifyOTPDTO request);

    BaseResponseDTO<UserResponseDTO> getUserByEmail(String email);

    BaseResponseDTO<LoginResponseDTO> login(String email, String password);

    BaseResponseDTO<List<UserResponseDTO>> findAllUsers();

}
