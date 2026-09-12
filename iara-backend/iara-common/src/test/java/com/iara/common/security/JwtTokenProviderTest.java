package com.iara.common.security;

import com.iara.common.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes reais do JwtTokenProvider: geracao, validacao de assinatura,
 * expiracao, deteccao de tipo de token incorreto e rejeicao de token
 * assinado com chave diferente (simulando token forjado).
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-with-at-least-256-bits-for-hmac-sha256-alg";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("iara-auth-test");
        properties.setAccessTokenValidityMs(900_000L);
        properties.setRefreshTokenValidityMs(2_592_000_000L);

        provider = new JwtTokenProvider(properties);
        provider.afterPropertiesSet();
    }

    @Test
    void createAccessToken_thenValidate_returnsCorrectSubjectAndRoles() {
        String token = provider.createAccessToken("usuario-123", List.of("ROLE_USER"));

        Claims claims = provider.validateAccessToken(token);

        assertThat(provider.extractSubject(claims)).isEqualTo("usuario-123");
        assertThat(provider.extractRoles(claims)).containsExactly("ROLE_USER");
    }

    @Test
    void createRefreshToken_thenValidate_returnsCorrectSubjectAndJti() {
        String token = provider.createRefreshToken("usuario-123", "jti-abc-123");

        Claims claims = provider.validateRefreshToken(token);

        assertThat(provider.extractSubject(claims)).isEqualTo("usuario-123");
        assertThat(provider.extractJti(claims)).isEqualTo("jti-abc-123");
    }

    @Test
    void validateAccessToken_withRefreshToken_throwsInvalidTokenException() {
        String refreshToken = provider.createRefreshToken("usuario-123", "jti-xyz");

        assertThatThrownBy(() -> provider.validateAccessToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void validateRefreshToken_withAccessToken_throwsInvalidTokenException() {
        String accessToken = provider.createAccessToken("usuario-123", List.of("ROLE_USER"));

        assertThatThrownBy(() -> provider.validateRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("refresh token");
    }

    @Test
    void validateAndParse_withTamperedToken_throwsInvalidTokenException() {
        String token = provider.createAccessToken("usuario-123", List.of("ROLE_USER"));
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";

        assertThatThrownBy(() -> provider.validateAndParse(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAndParse_withTokenSignedByDifferentKey_throwsInvalidTokenException() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("outra-chave-completamente-diferente-com-256-bits-minimo-aqui");
        otherProperties.setIssuer("iara-auth-test");
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProperties);
        otherProvider.afterPropertiesSet();

        String tokenFromOtherIssuer = otherProvider.createAccessToken("atacante", List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> provider.validateAndParse(tokenFromOtherIssuer))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAndParse_withExpiredToken_throwsInvalidTokenException() {
        JwtProperties shortLivedProperties = new JwtProperties();
        shortLivedProperties.setSecret(SECRET);
        shortLivedProperties.setIssuer("iara-auth-test");
        shortLivedProperties.setAccessTokenValidityMs(1L);
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(shortLivedProperties);
        shortLivedProvider.afterPropertiesSet();

        String token = shortLivedProvider.createAccessToken("usuario-123", List.of("ROLE_USER"));

        await(50);

        assertThatThrownBy(() -> shortLivedProvider.validateAndParse(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void validateAndParse_withMalformedToken_throwsInvalidTokenException() {
        assertThatThrownBy(() -> provider.validateAndParse("isto.nao.e-um-jwt-valido"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void afterPropertiesSet_withShortSecret_throwsIllegalStateException() {
        JwtProperties weakProperties = new JwtProperties();
        weakProperties.setSecret("chave-curta");
        JwtTokenProvider weakProvider = new JwtTokenProvider(weakProperties);

        assertThatThrownBy(weakProvider::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
