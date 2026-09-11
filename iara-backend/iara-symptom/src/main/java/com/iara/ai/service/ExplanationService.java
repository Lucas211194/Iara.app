package com.iara.ai.service;

import com.iara.ai.dto.ExplanationRequest;
import com.iara.ai.dto.ExplanationResponse;
import com.iara.ai.guard.ClinicalSafetyGuard;
import com.iara.cycle.entity.Ciclo;
import com.iara.cycle.repository.CicloRepository;
import com.iara.symptom.entity.Sintoma;
import com.iara.symptom.repository.SintomaRepository;
import com.iara.mood.entity.Humor;
import com.iara.mood.repository.HumorRepository;
import com.iara.sleep.entity.Sono;
import com.iara.sleep.repository.SonoRepository;
import com.iara.partner.service.PermissionGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExplanationService {

    private final CicloRepository cicloRepository;
    private final SintomaRepository sintomaRepository;
    private final HumorRepository humorRepository;
    private final SonoRepository sonoRepository;
    private final ClinicalSafetyGuard safetyGuard;
    private final PermissionGateService permissionGate;

    /**
     * IA EXPLICATIVA - REGRA IARA:
     * - Só descreve o que a usuária JÁ REGISTROU
     * - NUNCA infere causa emocional/física não confirmada
     * - NUNCA diagnostica
     * - Sinais de risco -> orienta buscar profissional (tom calmo)
     * - Modo parceiro: sem estereótipos, só fatos registrados + sugestão gentil
     */
    @Transactional(readOnly = true)
    public ExplanationResponse explainCycleStatus(ExplanationRequest request) {
        UUID userId = request.getUserId();
        String requesterId = request.getRequesterId(); // Pode ser parceiro
        boolean isPartner = requesterId != null && !requesterId.equals(userId.toString());

        // Verifica permissão se for parceiro
        if (isPartner && !permissionGate.canAccessCycleData(userId, UUID.fromString(requesterId))) {
            return ExplanationResponse.accessDenied();
        }

        // Busca dados REAIS registrados
        List<Ciclo> ciclos = cicloRepository.findByUsuarioIdOrderByNumeroCicloDesc(userId);
        List<Sintoma> sintomas = sintomaRepository.findByUsuarioIdAndDataRegistroBetween(
            userId, LocalDate.now().minusDays(30), LocalDate.now());
        List<Humor> humores = humorRepository.findByUsuarioIdAndDataRegistroBetween(
            userId, LocalDate.now().minusDays(30), LocalDate.now());
        List<Sono> sonos = sonoRepository.findByUsuarioIdAndDataRegistroBetween(
            userId, LocalDate.now().minusDays(30), LocalDate.now());

        // GUARDA DE SEGURANÇA CLÍNICA
        safetyGuard.assessRisk(sintomas, humores, sonos)
            .ifPresent(risk -> {
                // Retorna orientação calma para profissional
                throw new ClinicalRiskDetectedException(risk.getMessage());
            });

        // Constrói explicação baseada APENAS no registrado
        String explanation = buildExplanation(ciclos, sintomas, humores, sonos, isPartner);

        return ExplanationResponse.builder()
                .explanation(explanation)
                .dataPoints(buildDataPoints(ciclos, sintomas, humores, sonos))
                .isEstimate(true) // SEMPRE estimativa
                .disclaimer(buildDisclaimer(isPartner))
                .build();
    }

    private String buildExplanation(List<Ciclo> ciclos, List<Sintoma> sintomas, 
                                     List<Humor> humores, List<Sono> sonos, boolean isPartner) {
        StringBuilder sb = new StringBuilder();

        if (!ciclos.isEmpty()) {
            Ciclo atual = ciclos.get(0);
            sb.append(String.format("Você está no dia %d do ciclo %d. ", 
                atual.getDiaAtualDoCiclo(LocalDate.now()), atual.getNumeroCiclo()));
            
            if (atual.getDuracaoDias() != null) {
                double media = ciclos.stream()
                    .filter(c -> c.getDuracaoDias() != null)
                    .mapToInt(Ciclo::getDuracaoDias)
                    .average().orElse(0);
                sb.append(String.format("Seu ciclo atual tem %d dias. " +
                    "A média dos seus últimos %d ciclos completos foi %.0f dias. ", 
                    atual.getDuracaoDias(), ciclos.size(), media));
            }
        }

        // Sintomas - APENAS o que foi registrado
        if (!sintomas.isEmpty()) {
            sb.append("Nos últimos dias você registrou: ");
            sb.append(sintomas.stream()
                .map(s -> s.getNome() + " (intensidade " + s.getIntensidade() + "/10)")
                .reduce((a, b) -> a + "; " + b).orElse(""));
            sb.append(". ");
        }

        // MODO PARCEIRO: sem estereótipos, só fatos + sugestão gentil
        if (isPartner) {
            sb.append(buildPartnerGentleSuggestion(sintomas, humores, sonos));
        }

        return sb.toString();
    }

    private String buildPartnerGentleSuggestion(List<Sintoma> sintomas, List<Humor> humores, List<Sono> sonos) {
        StringBuilder sb = new StringBuilder();
        
        boolean cansaco = sintomas.stream().anyMatch(s -> 
            s.getNome().equalsIgnoreCase("FADIGA") || s.getNome().equalsIgnoreCase("CANSAÇO"));
        boolean dorIntensa = sintomas.stream().anyMatch(s -> s.getIntensidade() >= 8);
        boolean sonoRuim = sonos.stream().anyMatch(s -> s.getQualidade() != null && s.getQualidade() <= 3);

        if (cansaco || dorIntensa || sonoRuim) {
            sb.append("Ela registrou ");
            if (cansaco) sb.append("cansaço");
            if (dorIntensa) sb.append(cansaco ? " e dor intensa" : "dor intensa");
            if (sonoRuim) sb.append(cansaco || dorIntensa ? " e sono ruim" : "sono ruim");
            sb.append(" — talvez aprecie descanso e um gesto de cuidado. ");
        }

        return sb.toString();
    }

    private String buildDisclaimer(boolean isPartner) {
        String base = "Esta é uma explicação baseada nos SEUS registros — não é um diagnóstico médico. " +
                      "Previsões são ESTIMATIVAS estatísticas baseadas no seu histórico. " +
                      "Para qualquer preocupação com sua saúde, consulte um/a profissional.";
        
        if (isPartner) {
            base += " Esta visão respeita as permissões que ela escolheu compartilhar.";
        }
        return base;
    }
}