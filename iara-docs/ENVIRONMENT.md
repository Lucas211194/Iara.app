# Variaveis de Ambiente - Iara Backend

Este documento lista as variaveis de ambiente necessarias para compilar,
testar e executar o backend do Iara nesta fase do projeto (fix/mvp-bootstrap).

Nenhum valor real de producao deve ser commitado no repositorio.

## Modulo iara-common

| Variavel                       | Obrigatoria | Descricao                                                                 | Como gerar (exemplo local)                 |
|---------------------------------|-------------|----------------------------------------------------------------------------|---------------------------------------------|
| `IARA_COLUMN_ENCRYPTION_KEY`    | Sim         | Chave AES-256 (32 bytes) em Base64, usada pelo `ColumnEncryptor` para criptografar colunas sensiveis (saude reprodutiva, peso, anotacoes). | `openssl rand -base64 32`                   |
| `IARA_JWT_SECRET`               | Sim         | Segredo HMAC-SHA256 (minimo 256 bits / 32 caracteres) usado para assinar e validar tokens JWT (access e refresh). | `openssl rand -base64 48`                   |

### Regras de seguranca destas variaveis

- `IARA_COLUMN_ENCRYPTION_KEY` deve decodificar para **exatamente 32 bytes**.
  Se o tamanho for diferente, a aplicacao **nao sobe** (fail-fast, por design,
  em `ColumnEncryptor.afterPropertiesSet()`).
- `IARA_JWT_SECRET` deve ter **pelo menos 32 caracteres** (256 bits) — chaves
  menores sao rejeitadas no boot por `JwtTokenProvider.afterPropertiesSet()`.
- Nunca reutilize a mesma chave entre ambientes (dev/staging/producao).
- Rotacionar `IARA_JWT_SECRET` invalida todos os tokens emitidos ate entao
  (usuarios precisam logar novamente). Rotacionar `IARA_COLUMN_ENCRYPTION_KEY`
  exige uma migracao de re-criptografia dos dados existentes — nao suportado
  automaticamente nesta fase do projeto.

## Como configurar localmente

```bash
cd iara-backend
export IARA_COLUMN_ENCRYPTION_KEY=$(openssl rand -base64 32)
export IARA_JWT_SECRET=$(openssl rand -base64 48)
mvn -pl iara-common -am clean verify
```

## Variaveis previstas para proximas fases (ainda nao implementadas)

| Variavel                | Modulo previsto      | Status           |
|--------------------------|-----------------------|------------------|
| `POSTGRES_DB`            | iara-infra             | Nao implementado |
| `POSTGRES_USER`          | iara-infra             | Nao implementado |
| `POSTGRES_PASSWORD`      | iara-infra             | Nao implementado |
| `SPRING_DATASOURCE_URL`  | iara-auth, iara-user   | Nao implementado |

Nao adicione Kafka, Redis ou Keycloak nesta fase — decisao explicita para
manter o MVP no menor conjunto de infraestrutura possivel (apenas
PostgreSQL), conforme escopo desta rodada.
