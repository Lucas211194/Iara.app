package com.iara.common.exception;

/**
 * Lancada sempre que uma tentativa de leitura/escrita de dado classificado
 * como {@link com.iara.common.annotation.SensitiveHealthData} ocorre sem a
 * autorizacao necessaria (titular ausente, permissao revogada, permissao
 * inexistente ou categoria nunca compartilhavel).
 *
 * Esta excecao NUNCA deve incluir, na mensagem, o conteudo do dado sensivel
 * em si — apenas metadados (categoria, id do recurso), para nao vazar dados
 * de saude reprodutiva/intima em logs ou respostas de erro.
 */
public class SensitiveDataAccessException extends IaraException {

    private static final String ERROR_CODE = "SENSITIVE_DATA_ACCESS_DENIED";

    public SensitiveDataAccessException(String category) {
        super(ERROR_CODE, "Acesso negado ao dado sensivel da categoria: " + category);
    }

    public SensitiveDataAccessException(String category, String reason) {
        super(ERROR_CODE, "Acesso negado ao dado sensivel da categoria '" + category + "': " + reason);
    }
}
