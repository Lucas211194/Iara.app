package com.iara.cycle.service;

import com.iara.cycle.entity.Ciclo;
import com.iara.cycle.repository.CicloRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CyclePredictionServiceTest {

    @Mock
    CicloRepository cicloRepository;

    @InjectMocks
    CyclePredictionService predictionService;

    @Test
    void predictionShouldAlwaysBeLabeledAsEstimate() {
        // Given - 1 ciclo completo
        UUID userId = UUID.randomUUID();
        Ciclo ciclo = Ciclo.builder()
                .usuarioId(userId)
                .numeroCiclo(1)
                .dia1Inicio(LocalDate.of(2026, 1, 1))
                .duracaoDias(30)
                .completo(true)
                .build();

        when(cicloRepository.findByUsuarioIdOrIdentificadorInternoOrderByNumeroCiclo(userId, null))
                .thenReturn(List.of(ciclo));

        // When
        var prediction = predictionService.predictNextMenstruation(userId, null);

        // Then - REGRA IARA: Sempre "estimado", "previsão", nunca certeza
        assertThat(prediction.getPrevisaoProximaMenstruacao()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(prediction.getMensagemExplicativa())
                .contains("ESTIMATIVA")
                .contains("seu corpo pode variar");
        assertThat(prediction.isTemPadraoPessoal()).isFalse(); // Precisa 3 ciclos
    }

    @Test
    void personalPatternOnlyShownAfter3CompleteCycles() {
        // Given - 2 ciclos completos (menos que mínimo)
        UUID userId = UUID.randomUUID();
        List<Ciclo> ciclos = List.of(
            Ciclo.builder().usuarioId(userId).numeroCiclo(1).dia1Inicio(LocalDate.of(2026,1,1)).duracaoDias(28).completo(true).build(),
            Ciclo.builder().usuarioId(userId).numeroCiclo(2).dia1Inicio(LocalDate.of(2026,1,29)).duracaoDias(30).completo(true).build()
        );

        when(cicloRepository.findByUsuarioIdOrIdentificadorInternoOrderByNumeroCiclo(userId, null))
                .thenReturn(ciclos);

        // When
        boolean canShow = predictionService.canShowPersonalPatterns(userId, null);

        // Then - REGRA IARA: Mínimo 3 ciclos
        assertThat(canShow).isFalse();
    }

    @Test
    void personalPatternShownAfter3CompleteCycles() {
        // Given - 3 ciclos completos
        UUID userId = UUID.randomUUID();
        List<Ciclo> ciclos = List.of(
            Ciclo.builder().usuarioId(userId).numeroCiclo(1).dia1Inicio(LocalDate.of(2026,1,1)).duracaoDias(28).completo(true).build(),
            Ciclo.builder().usuarioId(userId).numeroCiclo(2).dia1Inicio(LocalDate.of(2026,1,29)).duracaoDias(30).completo(true).build(),
            Ciclo.builder().usuarioId(userId).numeroCiclo(3).dia1Inicio(LocalDate.of(2026,2,28)).duracaoDias(29).completo(true).build()
        );

        when(cicloRepository.findByUsuarioIdOrIdentificadorInternoOrderByNumeroCiclo(userId, null))
                .thenReturn(ciclos);

        // When
        boolean canShow = predictionService.canShowPersonalPatterns(userId, null);

        // Then
        assertThat(canShow).isTrue();
    }
}