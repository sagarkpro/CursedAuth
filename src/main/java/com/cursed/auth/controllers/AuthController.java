package com.cursed.auth.controllers;

import static com.cursed.auth.config.OpenApiConfig.BEARER_AUTH;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.dtos.LoginDTO;
import com.cursed.auth.dtos.RegisterDTO;
import com.cursed.auth.dtos.VerifyOTPDTO;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.dtos.response.SsoLoginResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.services.UserService;
import com.cursed.auth.utils.CommonUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "Register, verify, login, and inspect authenticated users.")
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/all")
    @Operation(summary = "List users", security = { @SecurityRequirement(name = BEARER_AUTH) })
    public ResponseEntity<BaseResponseDTO<List<UserResponseDTO>>> getAllUserResponseEntity() {
        return CommonUtils.handleResponse(userService.findAllUsers());
    }

    @GetMapping("/fake-login")
    @Operation(summary = "SSO login placeholder", description = "Explains how to continue a pending OAuth authorization-code request with JSON credentials.")
    public ResponseEntity<BaseResponseDTO<String>> fakeLogin(@RequestParam(required = false) String error) {
        if (error != null) {
            return CommonUtils.handleResponse(BaseResponseDTO.<String>builder()
                    .success(false)
                    .error(ErrorDTO.builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .message("Invalid email/password")
                            .build())
                    .build());
        }

        return CommonUtils.handleResponse(BaseResponseDTO.<String>builder()
                .success(true)
                .data("POST email and password as JSON to /api/auth/login to continue the pending /oauth2/authorize request.")
                .build());
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "SSO login", description = "Authenticates JSON credentials and continues the pending OAuth authorization-code request.")
    public ResponseEntity<BaseResponseDTO<SsoLoginResponseDTO>> login(@RequestBody @Valid LoginDTO request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SavedRequest savedRequest = requestCache.getRequest(httpRequest, httpResponse);
        if (savedRequest == null) {
            return CommonUtils.handleResponse(ssoLoginError(
                    "Start SSO with /oauth2/authorize before posting credentials.",
                    HttpStatus.BAD_REQUEST));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                SecurityContextHolder.clearContext();
                return CommonUtils.handleResponse(ssoLoginError(
                        "Start SSO with /oauth2/authorize before posting credentials.",
                        HttpStatus.BAD_REQUEST));
            }
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            return CommonUtils.handleResponse(BaseResponseDTO.<SsoLoginResponseDTO>builder()
                    .success(true)
                    .data(SsoLoginResponseDTO.builder()
                            .redirectUrl(savedRequest.getRedirectUrl())
                            .message("Authenticated. Call redirectUrl with the same cookie jar to receive the authorization code.")
                            .build())
                    .build());
        } catch (DisabledException e) {
            return CommonUtils.handleResponse(ssoLoginError("Email not verified", HttpStatus.UNAUTHORIZED));
        } catch (LockedException e) {
            return CommonUtils.handleResponse(ssoLoginError("User is blocked", HttpStatus.UNAUTHORIZED));
        } catch (BadCredentialsException e) {
            return CommonUtils.handleResponse(ssoLoginError("Invalid email/password", HttpStatus.UNAUTHORIZED));
        } catch (AuthenticationException e) {
            return CommonUtils.handleResponse(ssoLoginError("Authentication failed", HttpStatus.UNAUTHORIZED));
        }
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

    private BaseResponseDTO<SsoLoginResponseDTO> ssoLoginError(String message, HttpStatus status) {
        return BaseResponseDTO.<SsoLoginResponseDTO>builder()
                .success(false)
                .error(ErrorDTO.builder()
                        .status(status)
                        .message(message)
                        .build())
                .build();
    }
}
