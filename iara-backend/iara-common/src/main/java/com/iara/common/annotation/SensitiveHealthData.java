package com.iara.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classifica um campo (ou uma entidade inteira) como dado pessoal sensivel
 * de saude, no sentido da LGPD (art. 5, II) e do principio central do Iara:
 * "nunca compartilhar um dado de saude sem autorizacao explicita e granular".
 *
 * Esta e a UNICA fonte de verdade para classificacao de sensibilidade no
 * backend. Nenhum outro modulo deve manter uma lista paralela de "campos
 * sensiveis" — services de permissao (ex.: PermissionGateService, a ser
 * implementado no modulo iara-partner) devem inspecionar esta anotacao via
 * reflexao em vez de duplicar a logica de classificacao.
 *
 * Categorias que sao, por regra de produto, NUNCA compartilhaveis com
 * parceiro (mesmo no nivel de permissao mais alto) sao marcadas com
 * {@link #neverShareable()} = true. Isso e verificado pelo modulo de
 * parceria antes de qualquer leitura por terceiro.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveHealthData {

    /**
     * Categoria do dado sensivel, usada para granularidade de permissao
     * (PERMISSAO_COMPARTILHAMENTO opera por categoria, nao por campo).
     */
    Category category();

    /**
     * Se true (padrao), o dado so pode ser lido pelo proprio titular.
     * Terceiros (parceiro, camada de IA, relatorio para consulta medica)
     * precisam de uma permissao explicita e ativa registrada para a
     * categoria correspondente.
     */
    boolean restrictedToOwner() default true;

    /**
     * Se true, esta categoria NUNCA pode ser compartilhada com parceiro,
     * independentemente do nivel de permissao concedido (BASICO, APOIO ou
     * AVANCADO). Usado para: PESO, SAUDE_REPRODUTIVA, ANOTACAO_PESSOAL.
     */
    boolean neverShareable() default false;

    enum Category {
        REPRODUCTIVE_HEALTH,
        WEIGHT,
        PERSONAL_NOTE,
        MOOD,
        MENSTRUAL_CYCLE,
        SYMPTOM,
        SLEEP,
        ACTIVITY,
        PARTNER_MESSAGE
    }
}
