package com.iara.reproductive.entity;

import com.iara.common.annotation.SensitiveHealthData;
import com.iara.common.crypto.ColumnEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "saude_reprodutiva", schema = "iara_reproductive",
       indexes = @Index(name = "idx_saude_repro_usuario_data", columnList = "usuario_id, data_registro"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaudeReprodutiva {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "identificador_interno")
    private String identificadorInterno;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaSaudeReprodutiva categoria;

    @Column(name = "descricao", columnDefinition = "TEXT")
    @Convert(converter = ColumnEncryptor.EncryptedStringConverter.class)
    @SensitiveHealthData(
        category = SensitiveHealthData.SensitiveCategory.REPRODUCTIVE_HEALTH, 
        restrictedToOwner = true
    )
    private String descricao;

    // Campos estruturados para relatórios médicos (opcionais)
    @Column(name = "valor_quantitativo")
    private Double valorQuantitativo;

    @Column(name = "unidade_medida")
    private String unidadeMedida;

    @Column(name = "profissional_saude")
    private String profissionalSaude;

    @Column(name = "unidade_saude")
    private String unidadeSaude;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Version
    private Long version;

    public enum CategoriaSaudeReprodutiva {
        CONTRACEPCAO,           // Método contraceptivo, troca, efeitos
        PLANEJAMENTO_GRAVIDEZ,  // Tentativa, tratamento, exames
        GESTACAO,               // Acompanhamento gestacional
        POS_PARTO,              // Puerpério, amamentação
        MENOPAUSA,              // Sintomas, terapia hormonal
        CONDICAO_GINECOLOGICA,  // Endometriose, SOP, miomas, etc.
        EXAME_PREVENTIVO,       // Papanicolau, mamografia, ultrassom
        SAUDE_SEXUAL,           // IST, libido, dor, satisfação
        OUTRO
    }
}