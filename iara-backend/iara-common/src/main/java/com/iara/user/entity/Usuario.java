package com.iara.user.entity;

import com.iara.common.annotation.SensitiveHealthData;
import com.iara.common.crypto.ColumnEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario", schema = "iara_user")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    // Identificador interno para modo sem identidade vinculada
    @Column(name = "identificador_interno", unique = true, nullable = false, updatable = false)
    private String identificadorInterno; // UUID v4 gerado no cadastro anônimo

    // Dados de identidade civil - OPCIONAIS (modo anônimo)
    @Column(name = "nome_civil")
    private String nomeCivil;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telefone", unique = true)
    private String telefone;

    // OAuth2
    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "provider")
    private String provider; // GOOGLE, APPLE, EMAIL_PASSWORD

    // MFA
    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_backup_codes")
    @Convert(converter = ColumnEncryptor.EncryptedStringConverter.class)
    @SensitiveHealthData(category = SensitiveHealthData.SensitiveCategory.PERSONAL_NOTES)
    private String mfaBackupCodesEncrypted;

    // Controle de modo anônimo
    @Column(name = "modo_anonimo", nullable = false)
    private boolean modoAnonimo = true;

    @Column(name = "identidade_vinculada_em")
    private Instant identidadeVinculadaEm;

    // Auditoria
    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Version
    private Long version;

    // Métodos de conveniência
    public boolean hasLinkedIdentity() {
        return !modoAnonimo && (email != null || telefone != null || providerId != null);
    }

    public String getDisplayName() {
        if (nomeCivil != null && !nomeCivil.isBlank()) return nomeCivil;
        if (email != null) return email.split("@")[0];
        return "Iara " + identificadorInterno.substring(0, 8);
    }
}