package com.iara.partner.repository;

import com.iara.partner.entity.PermissaoCompartilhamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

public interface PermissaoCompartilhamentoRepository extends JpaRepository<PermissaoCompartilhamento, UUID> {

    boolean existsByOwnerIdAndParceiroIdAndCategoriaAndAtivaTrue(UUID ownerId, UUID parceiroId, String categoria);

    @Query("SELECT DISTINCT p.categoria FROM PermissaoCompartilhamento p " +
           "WHERE p.ownerId = :ownerId AND p.parceiroId = :parceiroId AND p.ativa = true AND p.revogadaEm IS NULL")
    Set<String> findActiveCategories(@Param("ownerId") UUID ownerId, @Param("parceiroId") UUID parceiroId);

    boolean existsActivePermission(@Param("ownerId") UUID ownerId, 
                                    @Param("parceiroId") UUID parceiroId,
                                    @Param("nivel") PermissaoCompartilhamento.Nivel nivel,
                                    @Param("categoria") String categoria);

    @Modifying
    @Transactional
    @Query("UPDATE PermissaoCompartilhamento p SET p.ativa = false, p.revogadaEm = CURRENT_TIMESTAMP, " +
           "p.revogadaPorOwnerId = :ownerId WHERE p.ownerId = :ownerId AND p.parceiroId = :parceiroId " +
           "AND (:categoria IS NULL OR p.categoria = :categoria)")
    int revokeByOwnerAndPartnerAndCategory(@Param("ownerId") UUID ownerId,
                                            @Param("parceiroId") UUID parceiroId,
                                            @Param("categoria") String categoria);
}