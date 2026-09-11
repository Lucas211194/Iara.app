// Compartilhado entre Mobile e Web
import React from 'react';
import { View, Text, StyleSheet, Pressable } from 'react-native';
import { useTheme } from '@iara/theme';

interface CycleDayCardProps {
  dayOfCycle: number;
  cycleNumber: number;
  nextPeriodEstimate?: Date;
  fertilityWindow?: { start: Date; end: Date };
  onPress?: () => void;
}

export const CycleDayCard: React.FC<CycleDayCardProps> = ({
  dayOfCycle,
  cycleNumber,
  nextPeriodEstimate,
  fertilityWindow,
  onPress,
}) => {
  const { colors, spacing, typography } = useTheme();
  const isFertile = fertilityWindow && 
    new Date() >= fertilityWindow.start && new Date() <= fertilityWindow.end;

  return (
    <Pressable onPress={onPress} style={styles.card}>
      <View style={styles.header}>
        <Text style={[styles.cycleDay, { color: colors.textPrimary }]}>
          Dia {dayOfCycle} do Ciclo {cycleNumber}
        </Text>
        {isFertile && (
          <Text style={styles.fertileBadge}>🌸 Janela fértil estimada</Text>
        )}
      </View>

      {nextPeriodEstimate && (
        <View style={styles.estimateRow}>
          <Text style={styles.estimateLabel}>Próxima menstruação</Text>
          <Text style={styles.estimateValue}>
            {nextPeriodEstimate.toLocaleDateString('pt-BR')}
            <Text style={styles.estimateDisclaimer}> (estimado)</Text>
          </Text>
        </View>
      )}

      <Text style={styles.disclaimer}>
        Previsão baseada no SEU histórico. Não é certeza.
      </Text>
    </Pressable>
  );
};

const styles = StyleSheet.create({
  card: {
    padding: 16,
    borderRadius: 16,
    backgroundColor: '#FFF',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 2,
  },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cycleDay: { fontSize: 20, fontWeight: '600' },
  fertileBadge: { fontSize: 12, backgroundColor: '#FFF0F5', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 12 },
  estimateRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 12 },
  estimateLabel: { fontSize: 14, color: '#666' },
  estimateValue: { fontSize: 16, fontWeight: '500' },
  estimateDisclaimer: { fontSize: 12, fontWeight: '400', color: '#888' },
  disclaimer: { marginTop: 12, fontSize: 11, color: '#999', textAlign: 'center' },
});