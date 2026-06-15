package com.cursed.auth.controllers;

import static com.cursed.auth.config.OpenApiConfig.BEARER_AUTH;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.dtos.LoginDto;
import com.cursed.auth.dtos.RegisterDto;
import com.cursed.auth.dtos.ResendOTPDto;
import com.cursed.auth.dtos.UpdatePasswordDto;
import com.cursed.auth.dtos.VerifyOTPDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.entities.LoginTransaction;
import com.cursed.auth.repository.LoginTransactionRepository;
import com.cursed.auth.services.TokenService;
import com.cursed.auth.services.UserService;
import com.cursed.auth.utils.CommonUtils;
import com.cursed.auth.utils.RedirectUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "Register, verify, login, and inspect authenticated users.")
public class AuthController {
    private final UserService userService;
    private final TokenService tokenService;
    private final LoginTransactionRepository loginTransactionRepository;

    public AuthController(UserService userService, TokenService tokenService,
            LoginTransactionRepository loginTransactionRepository) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.loginTransactionRepository = loginTransactionRepository;
    }

    @GetMapping("/all")
    @Operation(summary = "List users", security = { @SecurityRequirement(name = BEARER_AUTH) })
    public ResponseEntity<BaseResponseDTO<List<UserResponseDTO>>> getAllUserResponseEntity() {
        return CommonUtils.handleResponse(userService.findAllUsers());
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "JSON login. With a loginId (OAuth flow) it issues an "
            + "authorization code and returns the client redirect URL; otherwise it just authenticates.")
    public ResponseEntity<?> login(@RequestBody LoginDto request) {
        if (StringUtils.isNotBlank(request.getLoginId())) {
            return completeOAuthLogin(request);
        }
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
            @RequestBody @Valid RegisterDto user) {
        return CommonUtils.handleResponse(userService.register(user));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verifies a registration OTP and marks the user verified.")
    public ResponseEntity<BaseResponseDTO<LoginResponseDTO>> verifyOtp(@RequestBody @Valid VerifyOTPDto request) {
        return CommonUtils.handleResponse(userService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Regenerates and re-sends the email verification OTP for an unverified user.")
    public ResponseEntity<BaseResponseDTO<String>> resendOtp(@RequestBody @Valid ResendOTPDto request) {
        return CommonUtils.handleResponse(userService.resendOtp(request.getEmail()));
    }

    @PostMapping("{email}/help-a-goldfish-find-its-password")
    public ResponseEntity<BaseResponseDTO> resetPassword(@PathVariable String email) {
        return CommonUtils.handleResponse(userService.resetPassword(email));
    }

    @PatchMapping("/update-password")
    public ResponseEntity<BaseResponseDTO> updatePassword(@RequestBody @Valid UpdatePasswordDto request) {
        return CommonUtils.handleResponse(userService.updatePassword(request));
    }

    /**
     * OAuth authorization-code login. Resolves the login transaction, authenticates
     * the
     * user (reusing the standard credential checks), issues a single-use
     * authorization
     * code, and returns the client redirect URL in the body so the login app can
     * navigate
     * (a JSON fetch cannot follow a 302).
     */
    private ResponseEntity<BaseResponseDTO<Map<String, String>>> completeOAuthLogin(LoginDto request) {
        LoginTransaction tx = loginTransactionRepository.findById(request.getLoginId()).orElse(null);
        if (tx == null) {
            return oauthLoginError("invalid_login", "Login session expired or invalid. Please restart sign-in.");
        }
        UserService.AuthResult result = userService.authenticate(request.getEmail(), request.getPassword());
        if (result.user() == null) {
            return oauthLoginError("invalid_credentials", result.errorMessage());
        }

        String code = tokenService.issueAuthorizationCode(tx, result.user().getId(), Instant.now());
        loginTransactionRepository.deleteById(tx.getId());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        String state = StringUtils.trimToNull(tx.getState());
        if (state != null) {
            params.put("state", state);
        }
        String redirectUri = RedirectUtils.withParams(tx.getRedirectUri(), params).toString();

        return ResponseEntity.ok(BaseResponseDTO.<Map<String, String>>builder()
                .success(true)
                .data(Map.of("redirectUri", redirectUri))
                .build());
    }

    private ResponseEntity<BaseResponseDTO<Map<String, String>>> oauthLoginError(String code, String message) {
        return ResponseEntity.badRequest().body(BaseResponseDTO.<Map<String, String>>builder()
                .success(false)
                .error(ErrorDTO.builder().code(code).message(message).build())
                .build());
    }
}
