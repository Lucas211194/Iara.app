package com.iara.common.security;

import com.iara.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Emissao e validacao real de tokens JWT (assinatura HMAC-SHA256).
 *
 * "Validacao real" aqui significa: assinatura verificada criptograficamente,
 * emissor (issuer) conferido, expiracao checada pela biblioteca (nao por
 * comparacao manual de datas), e tipo de token (access/refresh) validado
 * explicitamente para impedir que um refresh token seja aceito onde um
 * access token e esperado (e vice-versa).
 *
 * A chave de assinatura NUNCA e hardcoded: vem de iara.jwt.secret, que por
 * sua vez deve ser preenchida pela variavel de ambiente IARA_JWT_SECRET.
 * O tamanho minimo de 256 bits (32 caracteres) e exigido no boot.
 */
@Component
public class JwtTokenProvider implements InitializingBean {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_ROLES = "roles";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        String secret = properties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                "iara.jwt.secret (variavel de ambiente IARA_JWT_SECRET) nao configurado.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalStateException(
                "IARA_JWT_SECRET deve ter pelo menos " + MIN_SECRET_LENGTH_BYTES +
                " bytes (256 bits) para uso seguro com HS256. Tamanho atual: " +
                keyBytes.length + " bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String subject, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getAccessTokenValidityMs());

        return Jwts.builder()
                .subject(subject)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    public String createRefreshToken(String subject, String jti) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getRefreshTokenValidityMs());

        return Jwts.builder()
                .subject(subject)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .id(jti)
                .signWith(signingKey)
                .compact();
    }

    public Claims validateAndParse(String token) {
        requireInitialized();
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token expirado.", e);
        } catch (SignatureException e) {
            throw new InvalidTokenException("Assinatura do token invalida.", e);
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Token malformado.", e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Token invalido.", e);
        }
    }

    public Claims validateAccessToken(String token) {
        Claims claims = validateAndParse(token);
        String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(type)) {
            throw new InvalidTokenException("Token fornecido nao e um access token valido.");
        }
        return claims;
    }

    public Claims validateRefreshToken(String token) {
        Claims claims = validateAndParse(token);
        String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_REFRESH.equals(type)) {
            throw new InvalidTokenException("Token fornecido nao e um refresh token valido.");
        }
        return claims;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        return List.of();
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    private void requireInitialized() {
        if (signingKey == null) {
            throw new IllegalStateException("JwtTokenProvider nao foi inicializado corretamente.");
        }
    }
}
