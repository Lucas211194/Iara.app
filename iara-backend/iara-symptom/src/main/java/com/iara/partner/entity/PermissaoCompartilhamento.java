package com.iara.partner.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permissao_compartilhamento", schema = "iara_partner",
       uniqueConstraints = @UniqueConstraint(name = "uk_permissao_owner_partner_categoria", 
           columnNames = {"owner_id", "parceiro_id", "categoria"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PermissaoCompartilhamento {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId; // Usuária titular dos dados

    @Column(name = "parceiro_id", nullable = false)
    private UUID parceiroId; // Usuário parceiro

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false)
    private Nivel nivel;

    @Column(name = "categoria", nullable = false, length = 50)
    private String categoria; // CICLO, SINTOMA, HUMOR, SONO, ATIVIDADE, SAUDE_REPRODUTIVA, PESO, ANOTACAO

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    @Column(name = "concedida_em", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant concedidaEm;

    @Column(name = "revogada_em")
    private Instant revogadaEm;

    @Column(name = "revogada_por_owner_id")
    private UUID revogadaPorOwnerId;

    public enum Nivel {
        BASICO,    // Previsão geral (próxima menstruação estimada)
        APOIO,     // + Mensagens curtas de necessidade ("quero companhia", "preciso descanso")
        AVANCADO   // + Fase estimada do ciclo, sintomas gerais (NUNCA peso, saúde reprodutiva, anotações)
    }

    // Categorias que NUNCA podem ser compartilhadas, mesmo no nível AVANCADO
    public static final Set<String> CATEGORIAS_NUNCA_COMPARTILHADAS = Set.of(
        "PESO", "SAUDE_REPRODUTIVA", "ANOTACAO"
    );

    public boolean isCategoriaNuncaCompartilhada() {
        return CATEGORIAS_NUNCA_COMPARTILHADAS.contains(categoria.toUpperCase());
    }

    public boolean isActive() {
        return ativa && revogadaEm == null;
    }
}