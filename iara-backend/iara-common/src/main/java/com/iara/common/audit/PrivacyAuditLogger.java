package com.iara.common.audit;

import com.iara.common.annotation.SensitiveHealthData;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Auditoria de privacidade - registra QUALQUER acesso a dados sensíveis.
 * 
 * REGRA IARA: Nenhum dado sensível sai do backend sem log de auditoria.
 * Bibliotecas de terceiros NUNCA recebem payload com dado de saúde.
 */
@Aspect
@Component
@Slf4j
public class PrivacyAuditLogger {

    @Pointcut("@annotation(com.iara.common.annotation.AuditableAccess)")
    public void auditableAccess() {}

    @Pointcut("execution(* com.iara.*.repository..*.find*(..)) || " +
              "execution(* com.iara.*.repository..*.findAll*(..)) || " +
              "execution(* com.iara.*.repository..*.get*(..))")
    public void repositoryReads() {}

    @AfterReturning(pointcut = "auditableAccess() || repositoryReads()", returning = "result")
    public void logAccess(JoinPoint joinPoint, Object result) {
        if (result == null) return;

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Detecta se resultado contém campos @SensitiveHealthData
        boolean hasSensitiveData = containsSensitiveData(result);
        
        AuditEvent event = AuditEvent.builder()
                .timestamp(Instant.now())
                .operation(methodName)
                .targetEntity(className)
                .hasSensitiveData(hasSensitiveData)
                .sensitiveCategories(extractSensitiveCategories(result))
                .build();

        // Log estruturado para SIEM / auditoria - NUNCA loga o conteúdo sensível
        log.info("PRIVACY_AUDIT: {}", event.toJson());
    }

    private boolean containsSensitiveData(Object obj) {
        if (obj == null) return false;
        if (obj.getClass().isAnnotationPresent(SensitiveHealthData.class)) return true;
        
        if (obj instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext() && containsSensitiveData(iterable.iterator().next());
        }
        
        return Arrays.stream(obj.getClass().getDeclaredFields())
                .anyMatch(f -> f.isAnnotationPresent(SensitiveHealthData.class));
    }

    private Set<SensitiveHealthData.SensitiveCategory> extractSensitiveCategories(Object obj) {
        // Extrai categorias para log de auditoria (sem valores)
        return Set.of(); // Implementação simplificada
    }
}