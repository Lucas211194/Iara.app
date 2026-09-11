package com.iara.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar campos que contêm dados de saúde sensíveis.
 * 
 * REGRAS DE PRIVACIDADE IARA:
 * - Campos anotados NUNCA podem ser enviados para bibliotecas de terceiros (analytics, ads, monitoring)
 * - Devem ser criptografados em nível de coluna (@Convert(converter = EncryptedStringConverter.class))
 * - Acesso restrito ao titular por padrão (acesso_restrito_titular = true)
 * - Compartilhamento só com PERMISSAO_COMPARTILHAMENTO explícita e ativa
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveHealthData {
    /**
     * Categoria do dado sensível para auditoria e controle de permissão.
     */
    SensitiveCategory category() default SensitiveCategory.GENERAL_HEALTH;

    /**
     * Se true (padrão), só o titular pode acessar. Parceiro/IA/relatório médico
     * precisam de PERMISSAO_COMPARTILHAMENTO explícita e ativa.
     */
    boolean restrictedToOwner() default true;

    enum SensitiveCategory {
        REPRODUCTIVE_HEALTH,      // Saúde reprodutiva / bem-estar íntimo
        WEIGHT_BODY_MEASURES,     // Peso, medidas corporais
        PERSONAL_NOTES,           // Anotações livres (texto)
        MENTAL_HEALTH,            // Humor, sintomas emocionais
        MENSTRUAL_DETAILS,        // Detalhes do fluxo menstrual
        SYMPTOMS,                 // Sintomas físicos
        SLEEP_PATTERNS,           // Padrões de sono
        PARTNER_COMMUNICATION     // Mensagens/necessidades compartilhadas com parceiro
    }
}