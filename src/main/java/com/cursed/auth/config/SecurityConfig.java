package com.cursed.auth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cursed.auth.exceptions.handlers.CustomAccessDeniedHandler;
import com.cursed.auth.exceptions.handlers.JwtAuthenticationEntryPoint;
import com.cursed.auth.filters.JwtFilter;

@Configuration
public class SecurityConfig {
	private final JwtFilter jwtFilter;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	public SecurityConfig(JwtFilter jwtFilter, CustomAccessDeniedHandler customAccessDeniedHandler,
			JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
		this.jwtFilter = jwtFilter;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity request) {
		return request.csrf(csrf -> csrf.disable())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(cors -> corsConfigurationSource())
				.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler)
						.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(
								"/api/auth/login",
								"/api/auth/register",
								"/api/auth/verify-otp",
								"/api/health/**")
						.permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.build();

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
