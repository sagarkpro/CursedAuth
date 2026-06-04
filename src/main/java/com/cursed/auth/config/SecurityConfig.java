package com.cursed.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cursed.auth.exceptions.handlers.CustomAccessDeniedHandler;
import com.cursed.auth.exceptions.handlers.JwtAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {
	private final CustomAccessDeniedHandler customAccessDeniedHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	public SecurityConfig(CustomAccessDeniedHandler customAccessDeniedHandler,
			JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.customAccessDeniedHandler = customAccessDeniedHandler;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

		http
				// 1. Tell Spring this chain ONLY handles Auth Server endpoints
				.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())

				// 2. PRE-EMPTIVE STRIKE: Add CORS here so your Next.js frontend can actually
				// make the POST request to /oauth2/token later!
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.httpBasic(AbstractHttpConfigurer::disable)

				// 3. Apply the OAuth2 config
				.with(authorizationServerConfigurer,
						(authorizationServer) -> authorizationServer.oidc(Customizer.withDefaults()))

				.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())

				// 4. Start the local login step when /oauth2/authorize is hit anonymously.
				.exceptionHandling((exceptions) -> exceptions
						.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/api/auth/fake-login")));

		return http.build();
	}

	// 2. The API Bouncer (Validates the JWTs you literally just issued)
	@Bean
	@Order(2)
	public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				// 1. Turn off CSRF (Safe because we are using stateless JWTs)
				.csrf(csrf -> csrf.disable())

				// 2. Keep sessions available for the JSON login step in the authorization-code flow
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

				// 3. Apply your CORS config
				// Note: Assuming corsConfigurationSource() is a bean or method in this class
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)

				// 4. Hook up your custom error handlers for 401s and 403s
				.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler)
						.authenticationEntryPoint(jwtAuthenticationEntryPoint))

				// 5. THE ROUTING RULES (Order matters here!)
				.authorizeHttpRequests(auth -> auth
						// Always let pre-flight CORS requests through
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// Allow public access to your custom Auth/Health/Swagger endpoints
						.requestMatchers(
								"/api/auth/fake-login",
								"/api/auth/login",
								"/api/auth/register",
								"/api/auth/verify-otp",
								"/api/auth/*/upload-profile-image",
								"/api/health/**",
								"/v3/api-docs/**",
								"/swagger-ui.html",
								"/swagger-ui/**")
						.permitAll()

						// CRITICAL FIX: Explicitly unblock the OAuth2 public endpoints!
						// This ensures clients can fetch your public keys and OIDC config without a
						// token
						.requestMatchers(
								"/.well-known/openid-configuration",
								"/oauth2/jwks",
								"/error")
						.permitAll()

						// Lock down everything else
						.anyRequest().authenticated())

				// 6. Tell Spring this is a Resource Server that accepts JWTs
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())

						// Optional: If you want your custom entry point to handle JWT failures too
						.authenticationEntryPoint(jwtAuthenticationEntryPoint));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		// ⚠️ allow all origins
		config.setAllowedOriginPatterns(List.of("*"));

		// allow all headers & methods
		config.setAllowedHeaders(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

		// important for Authorization header (JWT)
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
