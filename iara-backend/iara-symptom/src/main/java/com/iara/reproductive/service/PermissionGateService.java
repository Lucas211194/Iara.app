package com.iara.reproductive.service;

import com.iara.partner.entity.PermissaoCompartilhamento;
import com.iara.partner.repository.PermissaoCompartilhamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionGateService {

    private final PermissaoCompartilhamentoRepository permissaoRepository;

    /**
     * GATE DE PERMISSÃO - BLOQUEIA LEITURA NO BACKEND (não só na UI)
     * 
     * REGRA IARA: Controle de permissão do parceiro REALMENTE bloqueia leitura
     * no backend, não só esconde na interface.
     */
    @Transactional(readOnly = true)
    public boolean canAccessReproductiveHealth(UUID ownerId, UUID requesterId, String category) {
        // Proprietária sempre acessa
        if (ownerId.equals(requesterId)) return true;

        // Verifica permissão ativa e NÃO revogada
        return permissaoRepository.existsActivePermission(
            ownerId, 
            requesterId, 
            PermissaoCompartilhamento.Nivel.AVANCADO,
            "SAUDE_REPRODUTIVA"
        );
    }

    @Transactional(readOnly = true)
    public boolean canAccessPersonalNotes(UUID ownerId, UUID requesterId) {
        if (ownerId.equals(requesterId)) return true;
        return false; // ANOTACAO NUNCA compartilhada, nem no nível avançado
    }

    @Transactional(readOnly = true)
    public boolean canAccessWeight(UUID ownerId, UUID requesterId) {
        if (ownerId.equals(requesterId)) return true;
        return false; // PESO NUNCA compartilhado
    }

    @Transactional(readOnly = true)
    public Set<String> getAccessibleCategories(UUID ownerId, UUID requesterId) {
        if (ownerId.equals(requesterId)) {
            return Set.of("ALL");
        }
        
        return permissaoRepository.findActiveCategories(ownerId, requesterId);
    }

    /**
     * Revogação IMEDIATA - remove acesso no banco, invalida cache, notifica.
     */
    @Transactional
    public void revokeAccessImmediate(UUID ownerId, UUID partnerId, String category) {
        permissaoRepository.revokeByOwnerAndPartnerAndCategory(ownerId, partnerId, category);
        // TODO: Invalidar cache, notificar parceiro via WebSocket, log de auditoria
    }
}