package com.barrierfree.bf.global.auth;

import com.barrierfree.bf.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 실무 표준에 맞춘 JWT 생성 및 검증 컴포넌트 (JJWT 0.12.x 최신 스펙 적용)
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Base64로 인코딩된 문자열을 바이트 배열로 변환하여 안전한 SecretKey 객체 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     * 페이로드(Claims)에 유저 PK(id)와 권한(Role)을 담아, 매 요청마다 DB 조회 없이 인증되도록 설계
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
            .subject(String.valueOf(user.getId())) // 토큰 식별자로 유저 PK 사용
            .claim("role", user.getRole().getKey()) // 권한 정보 추가 (GUEST or USER)
            .claim("token_type", "access") // 토큰 타입 명시
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Refresh Token 생성
     * Access Token 갱신용이므로 최소한의 정보만 담습니다.
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim("token_type", "refresh") // 토큰 타입 명시
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }

    /**
     * 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰입니다. ({})", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token 전용 검증 메서드 - token_type이 "access"인지 확인합니다.
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get("token_type", String.class);
            return "access".equals(tokenType);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 Access Token입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 Access Token입니다. ({})", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh Token 전용 검증 메서드 - token_type이 "refresh"인지 확인합니다.
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String tokenType = claims.get("token_type", String.class);
            return "refresh".equals(tokenType);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 Refresh Token입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 Refresh Token입니다. ({})", e.getMessage());
        }
        return false;
    }

    /**
     * 검증된 토큰에서 유저 PK(ID)를 추출합니다.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 검증된 토큰에서 Role(권한) 정보를 추출합니다.
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.get("role", String.class);
    }
}