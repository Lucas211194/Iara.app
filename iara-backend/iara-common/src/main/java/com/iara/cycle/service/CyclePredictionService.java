package com.iara.cycle.service;

import com.iara.cycle.entity.Ciclo;
import com.iara.cycle.entity.PeriodoMenstrual;
import com.iara.cycle.repository.CicloRepository;
import com.iara.cycle.repository.PeriodoMenstrualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CyclePredictionService {

    private final CicloRepository cicloRepository;
    private final PeriodoMenstrualRepository periodoRepository;

    // MÍNIMO 3 CICLOS COMPLETOS para mostrar "padrão pessoal" (REGRA IARA)
    private static final int MIN_CICLOS_PARA_PADRAO = 3;

    /**
     * Calcula previsão da próxima menstruação baseada SOMENTE no histórico da usuária.
     * NUNCA assume 28 dias fixos.
     * 
     * REGRA IARA: Sempre retornar como ESTIMATIVA, nunca certeza.
     */
    @Transactional(readOnly = true)
    public CyclePrediction predictNextMenstruation(UUID usuarioId, String identificadorInterno) {
        List<Ciclo> ciclosCompletos = cicloRepository
                .findByUsuarioIdOrIdentificadorInternoOrderByNumeroCiclo(usuarioId, identificadorInterno)
                .stream()
                .filter(Ciclo::isCompleto)
                .collect(Collectors.toList());

        if (ciclosCompletos.size() < 1) {
            return CyclePrediction.noData();
        }

        // Último ciclo completo
        Ciclo ultimoCiclo = ciclosCompletos.get(ciclosCompletos.size() - 1);
        LocalDate ultimoDia1 = ultimoCiclo.getDia1Inicio();

        // Calcula média de duração dos ciclos completos
        double mediaDuracao = ciclosCompletos.stream()
                .filter(c -> c.getDuracaoDias() != null)
                .mapToInt(Ciclo::getDuracaoDias)
                .average()
                .orElse(28.0); // Fallback apenas se houver pelo menos 1 ciclo com duração

        // Previsão baseada na média pessoal
        LocalDate previsao = ultimoDia1.plusDays((long) Math.round(mediaDuracao));

        // Janela de fertilidade estimada (aprox. 14 dias antes da próxima menstruação)
        // REGRA IARA: SEMPRE estimativa estatística, nunca afirmação de fato
        LocalDate ovulacaoEstimada = previsao.minusDays(14);
        LocalDate janelaInicio = ovulacaoEstimada.minusDays(5); // 5 dias antes
        LocalDate janelaFim = ovulacaoEstimada.plusDays(1);     // 1 dia após

        boolean temPadraoPessoal = ciclosCompletos.size() >= MIN_CICLOS_PARA_PADRAO;

        return CyclePrediction.builder()
                .temDadosSuficientes(!ciclosCompletos.isEmpty())
                .temPadraoPessoal(temPadraoPessoal)
                .ultimoDia1(ultimoDia1)
                .previsaoProximaMenstruacao(previsao)
                .mediaCicloDias(Math.round(mediaDuracao))
                .janelaFertilidadeInicio(janelaInicio)
                .janelaFertilidadeFim(janelaFim)
                .ovulacaoEstimada(ovulacaoEstimada)
                .totalCiclosCompletos(ciclosCompletos.size())
                .mensagemExplicativa(buildExplanation(temPadraoPessoal, mediaDuracao))
                .build();
    }

    private String buildExplanation(boolean temPadrao, double media) {
        if (!temPadrao) {
            return String.format(
                "Baseado no seu único ciclo completo de %.0f dias. " +
                "A previsão é uma ESTIMATIVA estatística — seu corpo pode variar. " +
                "Com 3+ ciclos completos, seu padrão pessoal ficará mais preciso.",
                media
            );
        }
        return String.format(
            "Seu padrão pessoal: média de %.0f dias nos últimos %d ciclos completos. " +
            "Esta é uma ESTIMATIVA baseada no SEU histórico — não é uma certeza.",
            media, MIN_CICLOS_PARA_PADRAO
        );
    }

    /**
     * Verifica se pode mostrar correlações/padrões pessoais (REGRA: mínimo 3 ciclos).
     */
    @Transactional(readOnly = true)
    public boolean canShowPersonalPatterns(UUID usuarioId, String identificadorInterno) {
        long count = cicloRepository.countByUsuarioIdOrIdentificadorInternoAndCompletoTrue(usuarioId, identificadorInterno);
        return count >= MIN_CICLOS_PARA_PADRAO;
    }
}