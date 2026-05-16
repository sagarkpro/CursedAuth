package com.cursed.auth.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cursed.auth.dtos.LoginDTO;
import com.cursed.auth.dtos.RegisterDTO;
import com.cursed.auth.dtos.VerifyOTPDTO;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.entities.User;
import com.cursed.auth.services.UserService;
import com.cursed.auth.utils.CommonUtils;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    public ResponseEntity<BaseResponseDTO<List<UserResponseDTO>>> getAllUserResponseEntity() {
        return CommonUtils.handleResponse(userService.findAllUsers());
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponseDTO<LoginResponseDTO>> login(@RequestBody LoginDTO request) {
        return CommonUtils.handleResponse(userService.login(request.getEmail(), request.getPassword()));
    }

    @GetMapping("/{email}")
    public ResponseEntity<BaseResponseDTO<UserResponseDTO>> getUserByEmail(@PathVariable String email) {
        return CommonUtils.handleResponse(userService.getUserByEmail(email));
    }

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDTO<RegisterResponseDTO>> register(@RequestBody @Valid RegisterDTO request) {
        return CommonUtils.handleResponse(userService.register(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<BaseResponseDTO<LoginResponseDTO>> verifyOtp(@RequestBody @Valid VerifyOTPDTO request) {
        return CommonUtils.handleResponse(userService.verifyOtp(request));
    }
}
