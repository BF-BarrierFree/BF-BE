package com.barrierfree.bf.global.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** HTTP 요청이 들어올 때마다 헤더에서 JWT를 추출하고 검증하는 필터 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // 1. Request Header에서 토큰 추출
      String jwt = getJwtFromRequest(request);

      // 2. 토큰이 존재하고 유효한지 검사 (Access Token 전용 검증)
      if (StringUtils.hasText(jwt) && jwtProvider.validateAccessToken(jwt)) {

        // 3. 토큰에서 유저 ID 추출
        Long userId = jwtProvider.getUserIdFromToken(jwt);

        // 4. 토큰에서 Role 추출 및 권한 설정
        String role = jwtProvider.getRoleFromToken(jwt);

        // 실무 환경: DB 조회를 최소화하기 위해 토큰에 있는 정보만으로 임시 Authentication 객체를 만듭니다.
        // Role 정보를 토큰의 claim에서 추출하여 GUEST와 USER를 구분합니다.
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(role)));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 5. Spring Security Context에 인증 정보 저장 (이후 컨트롤러에서 @AuthenticationPrincipal로 꺼내 씀)
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    } catch (Exception ex) {
      log.error("Security Context에 사용자 인증을 설정할 수 없습니다.", ex);
    }

    // 5. 다음 필터로 요청 전달 (중요)
    filterChain.doFilter(request, response);
  }

  /** Authorization 헤더에서 'Bearer ' 접두사를 제거하고 순수 토큰만 추출합니다. */
  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // "Bearer " 이후의 문자열
    }
    return null;
  }
}
