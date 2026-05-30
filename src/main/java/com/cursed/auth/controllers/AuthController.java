package com.cursed.auth.controllers;

import static com.cursed.auth.config.OpenApiConfig.BEARER_AUTH;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.dtos.LoginDTO;
import com.cursed.auth.dtos.RegisterDTO;
import com.cursed.auth.dtos.VerifyOTPDTO;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.services.UserService;
import com.cursed.auth.utils.CommonUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "Register, verify, login, and inspect authenticated users.")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    @Operation(summary = "List users", security = { @SecurityRequirement(name = BEARER_AUTH) })
    public ResponseEntity<BaseResponseDTO<List<UserResponseDTO>>> getAllUserResponseEntity() {
        return CommonUtils.handleResponse(userService.findAllUsers());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates a verified active user and returns a JWT access token.")
    public ResponseEntity<BaseResponseDTO<LoginResponseDTO>> login(@RequestBody LoginDTO request) {
        return CommonUtils.handleResponse(userService.login(request.getEmail(), request.getPassword()));
    }

    @GetMapping("/{email}")
    @Operation(summary = "Get user by email", security = { @SecurityRequirement(name = BEARER_AUTH) })
    public ResponseEntity<BaseResponseDTO<UserResponseDTO>> getUserByEmail(@PathVariable String email) {
        return CommonUtils.handleResponse(userService.getUserByEmail(email));
    }

    @PostMapping(value = "{email}/upload-profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<String>> register(@PathVariable String email,
            @RequestPart("profileImage") MultipartFile profileImage) {
        return CommonUtils.handleResponse(userService.uploadUserProfile(email, profileImage));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register", description = "Creates a user account and sends an email verification OTP.")
    public ResponseEntity<BaseResponseDTO<RegisterResponseDTO>> register(
            @RequestBody @Valid RegisterDTO user) {
        return CommonUtils.handleResponse(userService.register(user));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies a registration OTP and returns a JWT access token.")
    public ResponseEntity<BaseResponseDTO<LoginResponseDTO>> verifyOtp(@RequestBody @Valid VerifyOTPDTO request) {
        return CommonUtils.handleResponse(userService.verifyOtp(request));
    }
}
