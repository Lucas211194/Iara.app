package com.iara.common.security;

import com.iara.common.annotation.SensitiveHealthData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAnalyticsGuardTest {

    @Mock
    ProceedingJoinPoint joinPoint;

    @InjectMocks
    ThirdPartyAnalyticsGuard guard;

    @Test
    void shouldBlockAnalyticsCallWhenPayloadContainsSensitiveHealthData() throws Throwable {
        // Given
        SensitiveDataPayload payload = new SensitiveDataPayload();
        payload.setCycleDay(14);
        payload.setSymptom("CÓLICA"); // Campo sensível
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(joinPoint.getSignature().getDeclaringTypeName()).thenReturn("com.google.firebase.analytics.FirebaseAnalytics");
        when(joinPoint.getSignature().getName()).thenReturn("logEvent");
        when(joinPoint.getSignature().getReturnType()).thenReturn(Void.TYPE);

        // When
        Object result = guard.sanitizeAnalyticsPayload(joinPoint);

        // Then
        assertThat(result).isNull(); // Bloqueado - retorna void seguro
        verify(joinPoint, never()).proceed(); // NÃO prossegue com a chamada original
    }

    @Test
    void shouldAllowAnalyticsCallWhenPayloadIsGenericUIEvent() throws Throwable {
        // Given
        GenericUIEvent payload = new GenericUIEvent("tela_aberta", "dashboard");
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(joinPoint.getSignature().getDeclaringTypeName()).thenReturn("com.google.firebase.analytics.FirebaseAnalytics");
        when(joinPoint.getSignature().getName()).thenReturn("logEvent");
        when(joinPoint.proceed()).thenReturn("ok");

        // When
        Object result = guard.sanitizeAnalyticsPayload(joinPoint);

        // Then
        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed(); // Prossegue normalmente
    }

    @Test
    void shouldBlockEvenAnonymizedHealthData() throws Throwable {
        // REGRA IARA: Nem anonimizado pode ir para analytics de terceiros
        AnonymizedHealthPayload payload = new AnonymizedHealthPayload();
        payload.setHashedUserId("abc123");
        payload.setCycleLength(28); // Dado de ciclo anonimizado
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(joinPoint.getSignature().getDeclaringTypeName()).thenReturn("com.amplitude.api.Amplitude");
        when(joinPoint.getSignature().getName()).thenReturn("logEvent");

        Object result = guard.sanitizeAnalyticsPayload(joinPoint);

        assertThat(result).isNull();
        verify(joinPoint, never()).proceed();
    }

    // Classes de teste internas
    @SensitiveHealthData(category = SensitiveHealthData.SensitiveCategory.MENSTRUAL_DETAILS)
    static class SensitiveDataPayload {
        private int cycleDay;
        private String symptom;
        // getters/setters
    }

    static class GenericUIEvent {
        private String eventName;
        private String screenName;
        public GenericUIEvent(String eventName, String screenName) { ... }
    }

    @SensitiveHealthData(category = SensitiveHealthData.SensitiveCategory.MENSTRUAL_DETAILS)
    static class AnonymizedHealthPayload {
        private String hashedUserId;
        private int cycleLength;
        // getters/setters
    }
}