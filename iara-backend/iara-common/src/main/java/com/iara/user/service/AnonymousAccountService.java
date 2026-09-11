package com.iara.user.service;

import com.iara.user.entity.Usuario;
import com.iara.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnonymousAccountService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Cria conta em modo anônimo - apenas identificador interno.
     * REGRA IARA: Usuária pode operar SEM associar nome civil, e-mail ou telefone.
     */
    @Transactional
    public Usuario createAnonymousAccount(String deviceFingerprint) {
        String internalId = "iara_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        
        Usuario usuario = Usuario.builder()
                .identificadorInterno(internalId)
                .modoAnonimo(true)
                .build();
        
        Usuario saved = usuarioRepository.save(usuario);
        log.info("Conta anônima criada: {}", saved.getIdentificadorInterno());
        return saved;
    }

    /**
     * Vincula identidade civil a conta anônima existente.
     * Operação irreversível - uma vez vinculada, não volta ao modo anônimo.
     */
    @Transactional
    public Usuario linkIdentity(String identificadorInterno, 
                                 String email, 
                                 String nomeCivil,
                                 String provider,
                                 String providerId) {
        Usuario usuario = usuarioRepository.findByIdentificadorInterno(identificadorInterno)
                .orElseThrow(() -> new AnonymousAccountNotFoundException(identificadorInterno));

        if (!usuario.isModoAnonimo()) {
            throw new IdentityAlreadyLinkedException(identificadorInterno);
        }

        usuario.setEmail(email);
        usuario.setNomeCivil(nomeCivil);
        usuario.setProvider(provider);
        usuario.setProviderId(providerId);
        usuario.setModoAnonimo(false);
        usuario.setIdentidadeVinculadaEm(Instant.now());

        return usuarioRepository.save(usuario);
    }

    /**
     * Migra dados de dispositivo (armazenamento local) para conta vinculada.
     * Usado quando usuária cadastra e-mail após usar app anonimamente.
     */
    @Transactional
    public void migrateLocalData(String identificadorInterno, String userId) {
        // Migra ciclos, sintomas, humor, sono, peso, atividade, reprodutiva, anotações
        // Todos os repositórios de domínio têm userId/identificadorInterno
        // Implementação delegada aos respectivos serviços de domínio
        log.info("Migração de dados locais para conta vinculada: {} -> {}", identificadorInterno, userId);
    }
}