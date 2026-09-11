package com.iara.auth.service;

import com.iara.auth.entity.RefreshToken;
import com.iara.auth.repository.RefreshTokenRepository;
import com.iara.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(String userId, String deviceFingerprint) {
        // Revoga tokens antigos do mesmo dispositivo (rotação)
        refreshTokenRepository.revokeByUserIdAndDevice(userId, deviceFingerprint);
        
        String jti = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenValidityMs());
        
        RefreshToken token = RefreshToken.builder()
                .id(jti)
                .userId(userId)
                .deviceFingerprint(deviceFingerprint)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();
        
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public Optional<RefreshToken> rotateRefreshToken(String currentJti, String deviceFingerprint) {
        return refreshTokenRepository.findById(currentJti)
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiryDate().isAfter(Instant.now()))
                .filter(token -> token.getDeviceFingerprint().equals(deviceFingerprint))
                .map(token -> {
                    // Revoga o token atual (rotação)
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                    
                    // Cria novo token
                    return createRefreshToken(token.getUserId(), deviceFingerprint);
                });
    }

    public boolean isAccessTokenRevoked(String accessToken) {
        // Extrai JTI do access token se houver, ou verifica se refresh token pai foi revogado
        // Implementação simplificada - em produção, usar cache Redis para blacklist de access tokens
        return false; // Access tokens curtos (15min) não mantêm blacklist; refresh token controla
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void revokeByDevice(String userId, String deviceFingerprint) {
        refreshTokenRepository.revokeByUserIdAndDevice(userId, deviceFingerprint);
    }

    // Limpeza diária de tokens expirados
    @Scheduled(cron = "0 3 * * *") // 03:00 AM
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Limpeza de refresh tokens expirados: {} removidos", deleted);
    }
}