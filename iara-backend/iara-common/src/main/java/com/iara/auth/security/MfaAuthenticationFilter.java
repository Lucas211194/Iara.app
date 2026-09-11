package com.iara.auth.security;

import com.iara.auth.entity.MfaDevice;
import com.iara.auth.repository.MfaDeviceRepository;
import com.iara.auth.service.TotpService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MfaAuthenticationFilter extends OncePerRequestFilter {

    private final MfaDeviceRepository mfaDeviceRepository;
    private final TotpService totpService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Só verifica MFA se já autenticado via JWT e acessando endpoint protegido
        if (auth != null && auth.isAuthenticated() && isProtectedEndpoint(request)) {
            String userId = auth.getName(); // subject do JWT = userId
            
            Optional<MfaDevice> activeDevice = mfaDeviceRepository
                .findByUserIdAndActiveTrue(userId);
            
            if (activeDevice.isPresent()) {
                String mfaCode = request.getHeader("X-MFA-Code");
                
                if (mfaCode == null || mfaCode.isBlank()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {"error": "MFA_REQUIRED", "message": "Código MFA necessário", "mfaType": "TOTP"}
                        """);
                    return;
                }
                
                if (!totpService.verify(activeDevice.get().getSecret(), mfaCode)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {"error": "MFA_INVALID", "message": "Código MFA inválido ou expirado"}
                        """);
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Endpoints que EXIGEM MFA (além do JWT)
        return path.startsWith("/api/v1/user/profile") ||
               path.startsWith("/api/v1/partner/") ||
               path.startsWith("/api/v1/reproductive/") ||
               path.startsWith("/api/v1/ai/explain") ||
               path.startsWith("/api/v1/export/");
    }
}