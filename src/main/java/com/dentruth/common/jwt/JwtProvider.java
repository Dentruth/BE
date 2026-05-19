package com.dentruth.common.jwt;

import com.dentruth.common.exception.JwtAuthenticationException;
import com.dentruth.common.response.code.ErrorStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    private static final long ACCESS_TOKEN_EXPIRE = 1000 * 60 * 60;
    private static final long REFRESH_TOKEN_EXPIRE = 1000 * 60 * 60 * 24 * 7;
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserId(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateAccessToken(String token) {
        try {
            String tokenType = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(TOKEN_TYPE_CLAIM, String.class);
            if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
                throw new JwtAuthenticationException(ErrorStatus.INVALID_TOKEN);
            }
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorStatus.EXPIRED_ACCESS_TOKEN);
        } catch (Exception e) {
            throw new JwtAuthenticationException(ErrorStatus.INVALID_TOKEN);
        }
    }

}
