package com.iara.ai.guard;

import com.iara.symptom.entity.Sintoma;
import com.iara.mood.entity.Humor;
import com.iara.sleep.entity.Sono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ClinicalSafetyGuard {

    /**
     * Detecta sinais de risco clínico que exigem orientação para profissional.
     * NUNCA diagnostica - apenas sinaliza para orientação calma.
     */
    public Optional<ClinicalRisk> assessRisk(List<Sintoma> sintomas, List<Humor> humores, List<Sono> sonos) {
        // 1. Sangramento fora do padrão (implementar verificação em Ciclo)
        // 2. Dor incapacitante (intensidade >= 9 por 2+ dias)
        // 3. Sofrimento emocional intenso (humor muito baixo persistente)
        // 4. Insônia severa persistente

        // Dor incapacitante
        long diasDorForte = sintomas.stream()
                .filter(s -> s.getIntensidade() >= 9)
                .map(Sintoma::getDataRegistro)
                .distinct()
                .count();
        
        if (diasDorForte >= 2) {
            return Optional.of(ClinicalRisk.builder()
                    .type(ClinicalRisk.Type.SEVERE_PAIN)
                    .message("Você registrou dor muito intensa por vários dias. " +
                            "Isso pode indicar necessidade de avaliação médica. " +
                            "Considere procurar um/a ginecologista ou clínico geral para investigar.")
                    .severity(ClinicalRisk.Severity.HIGH)
                    .build());
        }

        // Humor muito baixo persistente (possível depressão)
        long diasHumorBaixo = humores.stream()
                .filter(h -> h.getNivel() <= 2) // 1-10
                .map(Humor::getDataRegistro)
                .distinct()
                .count();
        
        if (diasHumorBaixo >= 14) { // 2 semanas
            return Optional.of(ClinicalRisk.builder()
                    .type(ClinicalRisk.Type.PERSISTENT_LOW_MOOD)
                    .message("Você registrou humor muito baixo por várias semanas. " +
                            "Isso pode ser sinal de sofrimento emocional que merece atenção. " +
                            "Conversar com um/a profissional de saúde mental ou médico/a pode ajudar. " +
                            "Você não precisa passar por isso sozinha.")
                    .severity(ClinicalRisk.Severity.HIGH)
                    .build());
        }

        return Optional.empty();
    }

    public record ClinicalRisk(Type type, String message, Severity severity) {
        public enum Type { SEVERE_PAIN, PERSISTENT_LOW_MOOD, ABNORMAL_BLEEDING, SEVERE_INSOMNIA }
        public enum Severity { MODERATE, HIGH, CRITICAL }
    }

    public static class ClinicalRiskDetectedException extends RuntimeException {
        public ClinicalRiskDetectedException(String message) {
            super(message);
        }
    }
}