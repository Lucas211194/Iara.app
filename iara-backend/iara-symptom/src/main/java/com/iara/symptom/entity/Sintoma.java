package com.iara.symptom.entity;

import com.iara.common.annotation.SensitiveHealthData;
import com.iara.common.crypto.ColumnEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sintoma", schema = "iara_symptom",
       indexes = {
           @Index(name = "idx_sintoma_usuario_data", columnList = "usuario_id, data_registro"),
           @Index(name = "idx_sintoma_interno_data", columnList = "identificador_interno, data_registro")
       })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Sintoma {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "identificador_interno")
    private String identificadorInterno;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;

    @Column(name = "ciclo_dia")
    private Integer cicloDia; // Dia do ciclo naquele momento

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private SintomaCategoria categoria;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome; // CÓLICA, DOR_CABEÇA, INCHAÇO, ACNE, FADIGA, etc.

    @Column(name = "intensidade", nullable = false)
    private Integer intensidade; // 1 a 10

    @Column(name = "duracao_horas")
    private Integer duracaoHoras;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    @Convert(converter = ColumnEncryptor.EncryptedStringConverter.class)
    @SensitiveHealthData(category = SensitiveHealthData.SensitiveCategory.SYMPTOMS, restrictedToOwner = true)
    private String observacoes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public enum SintomaCategoria {
        FISICO, EMOCIONAL, COGNITIVO, DERMATOLOGICO, DIGESTIVO, OUTRO
    }
}