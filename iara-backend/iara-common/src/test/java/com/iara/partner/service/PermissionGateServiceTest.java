package com.iara.partner.service;

import com.iara.partner.entity.PermissaoCompartilhamento;
import com.iara.partner.repository.PermissaoCompartilhamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionGateServiceTest {

    @Mock
    PermissaoCompartilhamentoRepository permissaoRepository;

    @InjectMocks
    PermissionGateService permissionGate;

    @Test
    void reproductiveHealthBlockedEvenAtAdvancedLevel() {
        // Given - permissão nível AVANÇADO para SAUDE_REPRODUTIVA
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        
        when(permissaoRepository.existsActivePermission(ownerId, partnerId, 
            PermissaoCompartilhamento.Nivel.AVANCADO, "SAUDE_REPRODUTIVA"))
            .thenReturn(true);

        // When
        boolean canAccess = permissionGate.canAccessReproductiveHealth(ownerId, partnerId, "SAUDE_REPRODUTIVA");

        // Then - REGRA IARA: NUNCA compartilha saúde reprodutiva, mesmo no avançado
        assertThat(canAccess).isFalse();
    }

    @Test
    void weightNeverShared() {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();

        when(permissaoRepository.existsActivePermission(ownerId, partnerId, 
            PermissaoCompartilhamento.Nivel.AVANCADO, "PESO"))
            .thenReturn(true);

        boolean canAccess = permissionGate.canAccessWeight(ownerId, partnerId);

        // REGRA IARA: Peso NUNCA compartilhado
        assertThat(canAccess).isFalse();
    }

    @Test
    void personalNotesNeverShared() {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();

        boolean canAccess = permissionGate.canAccessPersonalNotes(ownerId, partnerId);

        // REGRA IARA: Anotações NUNCA compartilhadas
        assertThat(canAccess).isFalse();
    }

    @Test
    void revokeAccessImmediatelyBlocksBackendRead() {
        UUID ownerId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();

        // Simula revogação
        when(permissaoRepository.revokeByOwnerAndPartnerAndCategory(ownerId, partnerId, "SINTOMA"))
            .thenReturn(1);

        permissionGate.revokeAccessImmediate(ownerId, partnerId, "SINTOMA");

        // Verifica que repositório foi chamado (bloqueio no backend)
        verify(permissaoRepository).revokeByOwnerAndPartnerAndCategory(ownerId, partnerId, "SINTOMA");
    }
}