package com.iara.cycle.entity;

import com.iara.common.annotation.SensitiveHealthData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ciclo", schema = "iara_cycle", 
       uniqueConstraints = @UniqueConstraint(name = "uk_ciclo_usuario_numero", columnNames = {"usuario_id", "numero_ciclo"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Ciclo {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "identificador_interno", nullable = false)
    private String identificadorInterno; // Para modo anônimo

    @Column(name = "numero_ciclo", nullable = false)
    private int numeroCiclo; // 1, 2, 3... baseado no histórico REAL da usuária

    @Column(name = "dia_1_inicio", nullable = false)
    private LocalDate dia1Inicio; // Primeiro dia de sangramento = Dia 1

    @Column(name = "duracao_dias")
    private Integer duracaoDias; // Calculado quando próximo ciclo começa

    @Column(name = "duracao_menstruacao_dias")
    private Integer duracaoMenstruacaoDias;

    @Column(name = "fluxo_intensidade_media")
    private String fluxoIntensidadeMedia; // LEVE, MODERADO, INTENSO, MUITO_INTENSO

    @Column(name = "observacoes", columnDefinition = "TEXT")
    @Convert(converter = ColumnEncryptor.EncryptedStringConverter.class)
    @SensitiveHealthData(category = SensitiveHealthData.SensitiveCategory.MENSTRUAL_DETAILS, restrictedToOwner = true)
    private String observacoes;

    @Column(name = "completo", nullable = false)
    private boolean completo = false; // True quando próximo ciclo começa

    @Column(name = "previsao_proxima_menstruacao")
    private LocalDate previsaoProximaMenstruacao; // SEMPRE estimativa

    @Column(name = "janela_fertilidade_inicio")
    private LocalDate janelaFertilidadeInicio; // ESTIMADA

    @Column(name = "janela_fertilidade_fim")
    private LocalDate janelaFertilidadeFim; // ESTIMADA

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Version
    private Long version;

    // Métodos de domínio
    public int getDiaAtualDoCiclo(LocalDate hoje) {
        if (hoje.isBefore(dia1Inicio)) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(dia1Inicio, hoje) + 1;
    }

    public boolean isFertilWindowEstimated(LocalDate date) {
        if (janelaFertilidadeInicio == null || janelaFertilidadeFim == null) return false;
        return !date.isBefore(janelaFertilidadeInicio) && !date.isAfter(janelaFertilidadeFim);
    }
}