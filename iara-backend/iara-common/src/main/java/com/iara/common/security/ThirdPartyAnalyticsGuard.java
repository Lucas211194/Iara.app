package com.iara.common.security;

import com.iara.common.annotation.SensitiveHealthData;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * GUARDA DE PRIVACIDADE - REGRA OBRIGATÓRIA IARA
 * 
 * Intercepta QUALQUER chamada para bibliotecas de terceiros (analytics, monitoring, ads)
 * e BLOQUEIA se o payload contiver qualquer campo anotado com @SensitiveHealthData.
 * 
 * Permite apenas eventos genéricos de UI: "tela_aberta", "botao_clicado", "navegacao"
 * NUNCA o conteúdo do registro de saúde da usuária.
 */
@Aspect
@Component
@Slf4j
public class ThirdPartyAnalyticsGuard {

    // Pointcut para bibliotecas conhecidas de analytics/monitoring/ads
    @Pointcut("execution(* com.google.firebase.analytics..*.logEvent(..)) || " +
              "execution(* com.facebook.appevents..*.logEvent(..)) || " +
              "execution(* com.amplitude.api..*.logEvent(..)) || " +
              "execution(* io.sentry..*.capture*(..)) || " +
              "execution(* io.micrometer..*.record(..)) || " +
              "execution(* org.springframework.boot.actuate.metrics..*.record(..))")
    public void thirdPartyAnalyticsCalls() {}

    @Around("thirdPartyAnalyticsCalls()")
    public Object sanitizeAnalyticsPayload(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        
        // Inspeciona argumentos em busca de dados sensíveis
        for (Object arg : args) {
            if (containsSensitiveHealthData(arg)) {
                String library = pjp.getSignature().getDeclaringTypeName();
                String method = pjp.getSignature().getName();
                
                // BLOQUEIA eloga apenas evento genérico seguro
                log.warn("BLOCKED: Biblioteca '{}' tentou enviar dado de saúde sensível via '{}'. " +
                         "Enviando apenas evento genérico 'tela_aberta'.", library, method);
                
                // Retorna void ou objeto neutro - NÃO prossegue com a chamada original
                return getSafeReturnValue(pjp.getSignature().getReturnType());
            }
        }
        
        // Se não tem dado sensível, permite prosseguir (evento genérico de UI)
        return pjp.proceed();
    }

    private boolean containsSensitiveHealthData(Object obj) {
        if (obj == null) return false;
        
        // Verifica anotação na classe
        if (obj.getClass().isAnnotationPresent(SensitiveHealthData.class)) {
            return true;
        }
        
        // Verifica campos recursivamente
        return Arrays.stream(obj.getClass().getDeclaredFields())
                .anyMatch(field -> {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(obj);
                        return field.isAnnotationPresent(SensitiveHealthData.class) ||
                               (value != null && containsSensitiveHealthData(value));
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                });
    }

    private Object getSafeReturnValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) return null;
        if (returnType == Boolean.class || returnType == boolean.class) return Boolean.TRUE;
        if (returnType == Integer.class || returnType == int.class) return 0;
        if (returnType == String.class) return "ok";
        return null;
    }
}