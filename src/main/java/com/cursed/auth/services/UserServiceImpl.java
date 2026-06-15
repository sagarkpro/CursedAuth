package com.cursed.auth.services;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cursed.auth.clients.BrevoEmailClient;
import com.cursed.auth.constants.MinIO;
import com.cursed.auth.constants.RedisKeys;
import com.cursed.auth.dtos.RegisterDto;
import com.cursed.auth.dtos.UpdatePasswordDto;
import com.cursed.auth.dtos.VerifyOTPDto;
import com.cursed.auth.dtos.response.BaseResponseDTO;
import com.cursed.auth.dtos.response.ErrorDTO;
import com.cursed.auth.dtos.response.LoginResponseDTO;
import com.cursed.auth.dtos.response.RegisterResponseDTO;
import com.cursed.auth.dtos.response.UserResponseDTO;
import com.cursed.auth.entities.User;
import com.cursed.auth.logging.CursedLogger;
import com.cursed.auth.domain.RedisOTPVerification;
import com.cursed.auth.domain.SendForgotPasswordEmail;
import com.cursed.auth.domain.SendOTPVerificationMail;
import com.cursed.auth.repository.UserRepository;
import com.cursed.auth.utils.CommonUtils;

import tools.jackson.databind.ObjectMapper;

@Service
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;
	private final RedisService redisService;
	private final ObjectMapper objectMapper;
	private final BrevoEmailClient brevoEmailClient;
	private final MinIOService minIOService;

	public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
			RedisService redisService, ObjectMapper objectMapper, BrevoEmailClient brevoEmailClient,
			MinIOService minIOService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.redisService = redisService;
		this.objectMapper = objectMapper;
		this.brevoEmailClient = brevoEmailClient;
		this.minIOService = minIOService;
	}

	public BaseResponseDTO<RegisterResponseDTO> register(RegisterDto request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			return BaseResponseDTO.<RegisterResponseDTO>builder()
					.success(false)
					.error(ErrorDTO.builder()
							.message("User already exists")
							.build())
					.build();
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			return BaseResponseDTO.<RegisterResponseDTO>builder()
					.success(false)
					.error(ErrorDTO.builder()
							.message("This username is taken by another user")
							.build())
					.build();
		}

		String imgUrl = redisService.getString(request.getEmail() + RedisKeys.USER_PROFILE_IMAGE);
		User user = User.builder()
				.createdAt(Instant.now())
				.email(request.getEmail())
				.firstName(request.getFirstName())
				.middleName(request.getMiddleName())
				.lastName(request.getLastName())
				.username(request.getUsername())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(request.getRole())
				.profileImage(imgUrl)
				.build();

		User res = userRepository.save(user);

		try {
			generateAndSendOtp(user);
		} catch (Exception ex) {
			return BaseResponseDTO.<RegisterResponseDTO>builder()
					.success(false)
					.error(ErrorDTO.builder()
							.message("Failed to send OTP Verification mail")
							.details(ex.getMessage()).build())
					.build();
		}

		return BaseResponseDTO.<RegisterResponseDTO>builder()
				.success(true)
				.data(RegisterResponseDTO.builder()
						.email(res.getEmail())
						.id(res.getId())
						.build())
				.build();
	}

	@Override
	public BaseResponseDTO<String> uploadUserProfile(String email, MultipartFile profileImage) {
		try {
			String imgUrl = minIOService.upload(profileImage, MinIO.USER_PROFILE_IMAGE_FOLDER);
			redisService.save(email + RedisKeys.USER_PROFILE_IMAGE, imgUrl);
			return BaseResponseDTO.<String>builder().success(true).data(imgUrl).build();
		} catch (IOException ex) {
			return BaseResponseDTO.<String>builder().success(false)
					.error(ErrorDTO.builder().message(ex.getMessage()).build()).build();
		}
	}

	@Override
	public BaseResponseDTO<UserResponseDTO> getUserByEmail(String email) {
		User user = userRepository.findByEmail(email);
		return BaseResponseDTO.<UserResponseDTO>builder()
				.data(toUserResponseDTO(user))
				.success(true)
				.build();
	}

	@Override
	public AuthResult authenticate(String email, String password) {
		User user = userRepository.findByEmail(email);
		if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
			return new AuthResult(null, "Invalid email/password");
		}
		if (!user.isVerified()) {
			return new AuthResult(null, "Email not verified");
		}
		if (!user.isActive()) {
			return new AuthResult(null, "User is blocked");
		}
		return new AuthResult(user, null);
	}

	@Override
	public BaseResponseDTO<LoginResponseDTO> login(String email, String password) {
		AuthResult result = authenticate(email, password);
		if (result.user() == null) {
			return BaseResponseDTO.<LoginResponseDTO>builder()
					.success(false)
					.error(ErrorDTO.builder().message(result.errorMessage()).build())
					.build();
		}
		// Tokens are not minted here; the OAuth flow mints them at /oauth/token.
		return BaseResponseDTO.<LoginResponseDTO>builder()
				.success(true)
				.data(LoginResponseDTO.builder().build())
				.build();
	}

	@Override
	public BaseResponseDTO<List<UserResponseDTO>> findAllUsers() {
		return BaseResponseDTO.<List<UserResponseDTO>>builder()
				.data(userRepository.findAll().stream().map(this::toUserResponseDTO).toList())
				.success(true)
				.build();
	}

	@Override
	public BaseResponseDTO<LoginResponseDTO> verifyOtp(VerifyOTPDto request) {
		var user = userRepository.findByEmail(request.getEmail());
		if (user != null && !user.isVerified()) {
			var otpVerification = redisService.getJson(user.getEmail() + RedisKeys.OTP_VERIFICATION);
			var otpVerificationRecord = objectMapper.treeToValue(otpVerification,
					RedisOTPVerification.class);
			if (otpVerificationRecord.getOtp().equals(request.getOtp())) {
				user.setVerified(true);
				userRepository.save(user);
				// String accessToken = jwtUtils.generateToken(user);
				return BaseResponseDTO.<LoginResponseDTO>builder()
						.success(true)
						.data(LoginResponseDTO.builder()
								// .accessToken(accessToken)
								.build())
						.build();
			} else {
				return BaseResponseDTO.<LoginResponseDTO>builder()
						.error(ErrorDTO.builder()
								.message("Invalid OTP").build())
						.build();
			}
		}
		return BaseResponseDTO.<LoginResponseDTO>builder()
				.error(ErrorDTO.builder()
						.message("Failed to verify user").build())
				.build();
	}

	@Override
	public BaseResponseDTO<String> resendOtp(String email) {
		User user = userRepository.findByEmail(email);
		if (user == null) {
			return BaseResponseDTO.<String>builder()
					.success(false)
					.error(ErrorDTO.builder().message("User not found").build())
					.build();
		}
		if (user.isVerified()) {
			return BaseResponseDTO.<String>builder()
					.success(false)
					.error(ErrorDTO.builder().message("User already verified").build())
					.build();
		}
		redisService.delete(user.getEmail() + RedisKeys.OTP_VERIFICATION);
		try {
			generateAndSendOtp(user);
		} catch (Exception ex) {
			return BaseResponseDTO.<String>builder()
					.success(false)
					.error(ErrorDTO.builder()
							.message("Failed to send OTP Verification mail")
							.details(ex.getMessage()).build())
					.build();
		}
		return BaseResponseDTO.<String>builder()
				.success(true)
				.data("OTP resent successfully")
				.build();
	}

	@Override
	public BaseResponseDTO resetPassword(String email) {
		User user = userRepository.findByEmail(email);
		if (user != null) {
			String superRandomToken = CommonUtils.generateSuperRandomString();
			redisService.save(email + RedisKeys.PASSWORD_RESET, superRandomToken, 10);
			try {
				brevoEmailClient.sendForgotPassword(SendForgotPasswordEmail
						.builder()
						.email(email)
						.receiverEmail(email)
						.receiverName(user.getDisplayName())
						.token(superRandomToken)
						.build());
			} catch (Exception ex) {
				return BaseResponseDTO.<String>builder()
						.success(false)
						.error(ErrorDTO.builder()
								.message("Failed to send password recovery mail")
								.details(ex.getMessage()).build())
						.build();
			}
		}
		return BaseResponseDTO.builder().success(true).build();
	}

	@Override
	public BaseResponseDTO updatePassword(UpdatePasswordDto request) {
		User user = userRepository.findByEmail(request.getEmail());
		if (user != null) {
			String key = request.getEmail() + RedisKeys.PASSWORD_RESET;
			String token = redisService.consumeOnceString(key);
			if (StringUtils.isNotBlank(token) && request.getToken().equals(token)) {
				String newPassword = passwordEncoder.encode(request.getPassword());
				user.setPassword(newPassword);
				userRepository.save(user);
				return BaseResponseDTO.builder().success(true).build();
			}
		}
		return BaseResponseDTO
				.builder()
				.success(false)
				.error(ErrorDTO
						.builder()
						.message("Invalid token")
						.build())
				.build();
	}

	private void generateAndSendOtp(User user) throws Exception {
		String otp = RandomStringUtils.secure().nextAlphanumeric(6).toUpperCase();
		CursedLogger.info("OTP for user " + user.getEmail() + " is: " + otp);
		redisService.save(user.getEmail() + RedisKeys.OTP_VERIFICATION, objectMapper.valueToTree(
				RedisOTPVerification.builder().otp(otp).email(user.getEmail()).build()));
		sendEmailVerificationOtp(user, otp);
	}

	private boolean sendEmailVerificationOtp(User user, String otp) throws Exception {
		var request = SendOTPVerificationMail.builder().receiverEmail(user.getEmail())
				.receiverName(user.getDisplayName()).OTP(otp).build();
		var response = brevoEmailClient.sendEmailVerificationOTP(request);
		if (response != null && !response.get("messageId").isEmpty()) {
			return true;
		}
		return false;
	}

	private UserResponseDTO toUserResponseDTO(User user) {
		return UserResponseDTO.builder()
				.email(user.getEmail())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.middleName(user.getMiddleName())
				.isActive(user.isActive())
				.role(user.getRole().toString())
				.displayName(Stream.of(
						user.getFirstName(),
						user.getMiddleName(),
						user.getLastName())
						.filter(StringUtils::isNotBlank)
						.collect(Collectors.joining(" ")))
				.build();
	}
}
