package com.cursed.auth.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cursed.auth.exceptions.handlers.CustomAccessDeniedHandler;
import com.cursed.auth.exceptions.handlers.JwtAuthenticationEntryPoint;

/**
 * Two stateless filter chains. Chain 1 (order 1) is the public OAuth/OIDC
 * surface and the
 * unauthenticated APIs — no bearer filter runs there. Chain 2 (order 2,
 * default) protects
 * everything else (/userinfo and the authenticated user APIs) as a JWT resource
 * server,
 * validating the RS256 access tokens we mint at /oauth/token.
 *
 * The default Spring Authorization Server endpoints (/oauth2/*) no longer
 * exist: the
 * OAuth2AuthorizationServerConfigurer has been removed and the dependency
 * dropped.
 */
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
	public SecurityFilterChain publicOidcFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher(
						"/authorize",
						"/login",
						"/oauth/token",
						"/v2/logout",
						"/.well-known/**",
						"/api/auth/login",
						"/api/auth/register",
						"/api/auth/verify-otp",
						"/api/auth/*/upload-profile-image",
						"/api/auth/*/help-a-goldfish-find-its-password",
						"/api/auth/update-password",
						"/api/client/**",
						"/api/health/**",
						"/error",
						"/v3/api-docs/**",
						"/swagger-ui.html",
						"/swagger-ui/**")
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(Customizer.withDefaults())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().permitAll())
				.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(Customizer.withDefaults())
				.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler)
						.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${sso.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		// Explicit origins only. The SPA uses Bearer headers (not cookies), so
		// credentials
		// are off — which also means a wildcard origin would be invalid anyway.
		config.setAllowedOrigins(allowedOrigins);
		// Reflect any requested header. auth0-spa-js sends a custom "Auth0-Client"
		// telemetry
		// header on /oauth/token; a narrow list rejects the preflight (403, no
		// Allow-Origin).
		// Valid because allowCredentials is false.
		config.setAllowedHeaders(List.of("*"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
