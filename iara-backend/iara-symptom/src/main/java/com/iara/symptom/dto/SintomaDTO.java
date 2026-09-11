package com.iara.symptom.dto;

import com.iara.symptom.entity.Sintoma;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SintomaDTO {
    private UUID id;
    private UUID usuarioId;
    private String identificadorInterno;
    private LocalDate dataRegistro;
    private Integer cicloDia;
    private Sintoma.SintomaCategoria categoria;
    private String nome;
    private Integer intensidade; // 1-10
    private Integer duracaoHoras;
    private String observacoes; // JÁ DESCRIPTOGRAFADO pelo converter JPA

    public static SintomaDTO fromEntity(Sintoma entity) {
        return SintomaDTO.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .identificadorInterno(entity.getIdentificadorInterno())
                .dataRegistro(entity.getDataRegistro())
                .cicloDia(entity.getCicloDia())
                .categoria(entity.getCategoria())
                .nome(entity.getNome())
                .intensidade(entity.getIntensidade())
                .duracaoHoras(entity.getDuracaoHoras())
                .observacoes(entity.getObservacoes())
                .build();
    }
}