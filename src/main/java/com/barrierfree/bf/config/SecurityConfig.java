package com.barrierfree.bf.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Provides a BCrypt-based password encoder.
   *
   * @return a password encoder using BCrypt hashing
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures CORS settings for development and local environments.
   *
   * @return a CorsConfigurationSource configured to allow GitHub Codespaces and
   *         localhost origins with support for standard HTTP methods and credentials
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // 프론트엔드 및 Codespaces 도메인 허용
    config.setAllowedOriginPatterns(
        List.of(
            "https://*.app.github.dev", // Codespace 환경
            "http://localhost:*", // 로컬 개발
            "https://localhost:*",
            "http://127.0.0.1:*"));

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * Configures HTTP security policies, CORS rules, and request authorization for the API.
   *
   * @return the configured security filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 위에서 정의한 정교한 CORS 설정 적용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // REST API이므로 CSRF 비활성화
        .csrf(csrf -> csrf.disable())
        // JWT를 사용할 예정이므로 세션을 생성하지 않음 (STATELESS)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 경로별 접근 권한 설정
        .authorizeHttpRequests(
            auth ->
                auth
                    // 비동기 요청 처리 허용
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC)
                    .permitAll()
                    // OPTIONS 요청(Preflight) 항상 허용 (가장 중요)
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    // 헬스체크 및 추후 오픈할 경로는 인증 없이 접근 허용
                    .requestMatchers("/", "/api/health", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    // 나머지 모든 요청은 우선 인증 필요
                    .anyRequest()
                    .authenticated());

    // 추후 JWT 개발 시 여기에 필터(.addFilterBefore)가 추가됩니다.

    return http.build();
  }
}
