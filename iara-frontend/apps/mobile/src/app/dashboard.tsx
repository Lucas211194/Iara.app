import React, { useEffect, useState } from 'react';
import { View, ScrollView, RefreshControl, Text } from 'react-native';
import { useCycleStore } from '@/store/cycle';
import { useSymptomStore } from '@/store/symptom';
import { useMoodStore } from '@/store/mood';
import { CycleDayCard } from '@iara/ui/components/CycleDayCard';
import { QuickLogButton } from '@iara/ui/components/QuickLogButton';
import { useTheme } from '@iara/theme';

export default function DashboardScreen() {
  const { colors, spacing } = useTheme();
  const { currentCycle, prediction, loadCycleData } = useCycleStore();
  const { logSymptom } = useSymptomStore();
  const { logMood } = useMoodStore();
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    loadCycleData();
  }, []);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadCycleData();
    setRefreshing(false);
  };

  return (
    <ScrollView
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
      contentContainerStyle={styles.container}
      showsVerticalScrollIndicator={false}
    >
      {/* Card Principal - Registro em ~1 minuto */}
      <CycleDayCard
        dayOfCycle={currentCycle?.dayOfCycle || 0}
        cycleNumber={currentCycle?.cycleNumber || 0}
        nextPeriodEstimate={prediction?.nextPeriodEstimate}
        fertilityWindow={prediction?.fertilityWindow}
        onPress={() => router.push('/cycle/detail')}
      />

      {/* Botões de registro rápido */}
      <View style={styles.quickLogRow}>
        <QuickLogButton 
          icon="😊" 
          label="Humor" 
          onPress={() => logMood({ level: 7, notes: '' })}
          color={colors.mood}
        />
        <QuickLogButton 
          icon="🩸" 
          label="Fluxo" 
          onPress={() => router.push('/cycle/log-flow')}
          color={colors.flow}
        />
        <QuickLogButton 
          icon="💊" 
          label="Sintoma" 
          onPress={() => router.push('/symptoms/log')}
          color={colors.symptom}
        />
        <QuickLogButton 
          icon="😴" 
          label="Sono" 
          onPress={() => router.push('/sleep/log')}
          color={colors.sleep}
        />
      </View>

      {/* Insight da IA (se tiver dados) */}
      {currentCycle && prediction?.hasPersonalPattern && (
        <View style={styles.aiInsight}>
          <Text style={styles.aiTitle}>💜 Iara explica</Text>
          <Text style={styles.aiText}>
            {prediction.explanation}
          </Text>
        </View>
      )}

      {/* Disclaimer permanente */}
      <Text style={styles.footerDisclaimer}>
        Iara é sua companheira, não vigilante. Seus dados são seus — 
        criptografados, nunca compartilhados sem seu consentimento explícito.
      </Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 16 },
  quickLogRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'space-between' },
  aiInsight: { backgroundColor: '#FDF0FF', borderRadius: 12, padding: 16 },
  aiTitle: { fontWeight: '600', fontSize: 16, marginBottom: 8 },
  aiText: { fontSize: 14, lineHeight: 22 },
  footerDisclaimer: { textAlign: 'center', fontSize: 11, color: '#999', marginTop: 24 },
});