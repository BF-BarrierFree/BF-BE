package com.barrierfree.bf.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${cors.allowed-origins}")
  private List<String> allowedOrigins;

  // 비밀번호 암호화를 위한 필수 빈 (추후 로그인 구현 시 사용)
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // CORS(도메인 간 접근) 허용 설정 (환경별 설정 적용)
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // 환경별 설정 파일에서 허용 도메인 가져오기
    config.setAllowedOrigins(allowedOrigins);

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

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
                    .requestMatchers(
                        "/",
                        "/api/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/v1/test/places/**",
                        "/api/v1/routes/**", // 로그인 없어도 가능한 기능이라 열어둠.
                        "/api/v1/test/mobility/**" // 교통약자 이동지원 테스트 API
                        )
                    .permitAll()
                    // 나머지 모든 요청은 우선 인증 필요
                    .anyRequest()
                    .authenticated());

    // 추후 JWT 개발 시 여기에 필터(.addFilterBefore)가 추가됩니다.

    return http.build();
  }
}
