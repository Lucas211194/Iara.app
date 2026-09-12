package com.iara.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Propriedades tipadas do JWT, vinculadas ao prefixo "iara.jwt" no
 * application.yml. Todos os valores sensiveis (secret) sao resolvidos a
 * partir de variaveis de ambiente, nunca hardcoded.
 */
@Validated
@ConfigurationProperties(prefix = "iara.jwt")
public class JwtProperties {

    @NotBlank(message = "iara.jwt.secret (env IARA_JWT_SECRET) e obrigatorio")
    private String secret;

    @NotBlank
    private String issuer = "iara-auth";

    @Positive
    private long accessTokenValidityMs = 900_000L;

    @Positive
    private long refreshTokenValidityMs = 2_592_000_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    public void setAccessTokenValidityMs(long accessTokenValidityMs) {
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    public void setRefreshTokenValidityMs(long refreshTokenValidityMs) {
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }
}
