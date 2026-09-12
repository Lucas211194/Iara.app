# Iara.app
# Iara — Bem-estar & Ciclo Menstrual

> **"O app é uma companheira, não um vigilante. Ele explica o corpo da usuária para ela mesma, nunca diagnostica, nunca julga, e nunca compartilha um dado de saúde sem autorização explícita e granular."**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React Native](https://img.shields.io/badge/React%20Native-Expo-61DAFB?logo=react&logoColor=black)](https://reactnative.dev/)
[![Next.js](https://img.shields.io/badge/Next.js-14-000000?logo=next.js&logoColor=white)](https://nextjs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🎯 Visão Geral

**Iara** é um aplicativo de bem-estar e acompanhamento de ciclo menstrual construído com **privacidade por design (Privacy by Design)** e **arquitetura modular**.  

- **Para a usuária:** registro rápido (≈1 min), explicações claras do próprio corpo, previsões sempre como *estimativas*, nenhum julgamento.  
- **Para o desenvolvedor:** backend Spring Boot isolado por domínio, criptografia em coluna (AES-GCM), zero analytics de terceiros com dados de saúde, testes de arquitetura (ArchUnit) garantindo as regras.  
- **Para o ecossistema:** modo anônimo (sem e-mail/telefone), compartilhamento gradual com parceiro(a) com revogação imediata no backend, camada de IA explicativa com guardas clínicas.

---

## 🔐 Princípios de Privacidade (Non-Negotiables)

| Regra | Implementação |
|-------|---------------|
| **Nenhum dado sensível sai do backend para libs de terceiros** | `ThirdPartyAnalyticsGuard` (AspectJ) bloqueia payloads com `@SensitiveHealthData` em Firebase/Amplitude/Sentry/etc. |
| **Criptografia em nível de coluna** | `@Convert(ColumnEncryptor.EncryptedStringConverter.class)` + AES-GCM (IV único por linha) em `saude_reprodutiva`, `peso`, `anotacao`, `sintomas`, `humor`, `sono`. |
| **Modo anônimo real** | `Usuario.modoAnonimo=true` + `identificadorInterno` (UUID). Dados ficam vinculados só a esse ID. Vinculação de identidade civil é opcional e irreversível. |
| **Permissões de parceiro no backend** | `PermissionGateService` valida *cada* leitura. Revogação = `UPDATE permissao SET ativa=false, revogada_em=now()` → corte imediato, não só UI. |
| **IA explicativa, nunca diagnóstica** | `ExplanationService` só descreve o que a usuária registrou. `ClinicalSafetyGuard` intercepta sinais de risco → orienta procurar profissional (tom calmo). |
| **Previsões sempre como estimativa** | Textos: "previsão", "estimado", "possível". Mínimo **3 ciclos completos** para mostrar "padrão pessoal". |
| **Nenhum estereótipo de gênero/humor** | Textos de IA e notificações revisados. Ex.: parceiro vê "ela registrou cansaço — talvez aprecie descanso e um gesto de cuidado". |

---

## 🏗️ Arquitetura

│ CLIENTES │
│ ┌──────────────────┐ ┌──────────────────┐ │
│ │ React Native │ │ Next.js 14 │ │
│ │ (Expo / Mobile) │ │ (Web / PWA) │ │
│ └────────┬─────────┘ └────────┬─────────┘ │
└───────────┼──────────────────────────────────┼───────────────────────┘
│ HTTPS / JSON (JWT + Device FP) │
▼ ▼
┌──────────────────────────────────────────────────────────────────────┐
│ API GATEWAY (Spring Cloud Gateway) │
│ Rate-limit - TLS - Roteamento por domínio - AuthZ centralizado │
└───────────┬──────────────────────────────────┬───────────────────────┘
│ │
┌──────▼──────┐ ┌──────▼──────┐
│ MÓDULOS │ Eventos de │ MÓDULOS │ (outros domínios)
│ DE DOMÍNIO │ domínio (Kafka) │ DE APOIO │
├─────────────┤ ├─────────────┤
│ iara-cycle │ │ iara-auth │
│ iara-symptom│ │ iara-user │
│ iara-mood │ │ iara-partner│
│ iara-sleep │ │ iara-ai │
│ iara-weight │ │ iara-alert │
│ iara-activity │ iara-reproductive│
│ iara-reproductive └─────────────────┘
└─────────────┘
│
▼
┌──────────────────────────────────────────────────────────────────────┐
│ POSTGRESQL (um banco, schemas isolados) │
│ iara_auth │ iara_user │ iara_cycle │ iara_symptom │ iara_mood ... │
│ Column-level encryption (pgcrypto / app-level AES-GCM) │


### Isolamento de Módulo (Regra Obrigatória)
- **Nenhum módulo acessa `repository` de outro módulo.**  
- Comunicação via **eventos de domínio** (Kafka/Outbox) ou **feign clients** expostos pelo próprio módulo dono do dado.  
- Testado com **ArchUnit** (`ArchitectureRulesTest`).

---

## 🧱 Stack Técnica

| Camada | Tecnologia | Versão |
|--------|------------|--------|
| **Backend** | Java + Spring Boot | 21 / 3.2.x |
| **Build** | Maven Multi-module | 3.9+ |
| **Auth** | Keycloak (OAuth2/OIDC) + JWT (HS256) + MFA (TOTP) | 24.x |
| **Banco** | PostgreSQL | 16 |
| **Migração** | Flyway (por módulo) | 10.x |
| **Crypto** | BouncyCastle (AES-GCM 256) | 1.78 |
| **Testes** | JUnit 5, Testcontainers, ArchUnit, RestAssured, WireMock | Latest |
| **Mobile** | React Native + Expo (TypeScript) | 50 / SDK 49 |
| **Web** | Next.js 14 (App Router) + Tailwind + SWR | 14.x |
| **Monorepo Front** | Turborepo + pnpm | 1.13 / 8.x |
| **UI Compartilhada** | `@iara/ui` (React Native Web compatible) | Internal |
| **Infra Local** | Docker Compose (Postgres, Redis, Keycloak, MinIO) | — |

---

## 📁 Estrutura do Repositório

```text
iara-app/
├── .github/workflows/           # CI/CD (build, test, archunit, dependency-check)
├── iara-backend/                # Maven multi-module
│   ├── pom.xml                  # Parent POM (dependencyManagement)
│   ├── iara-common/             # Kernel compartilhado (segurança, crypto, auditoria, guards)
│   ├── iara-auth/               # OAuth2, JWT, MFA, Refresh Token Rotation, Anonymous Mode
│   ├── iara-user/               # Perfil, modo anônimo, vinculação identidade
│   ├── iara-cycle/              # Ciclo menstrual, previsões, janela fértil (estimada)
│   ├── iara-symptom/            # Sintomas (físicos, emocionais, cognitivos)
│   ├── iara-mood/               # Humor, energia
│   ├── iara-sleep/              # Sono, qualidade, duração
│   ├── iara-weight/             # Peso, hidratação (criptografado)
│   ├── iara-activity/           # Atividade física
│   ├── iara-reproductive/       # Saúde reprodutiva (acesso restrito por padrão)
│   ├── iara-partner/            # Vínculo parceiro, permissões graduais (básico→apoio→avançado)
│   ├── iara-alert/              # Alertas de saúde (baseados em regras + IA)
│   └── iara-ai/                 # Camada explicativa + ClinicalSafetyGuard
├── iara-frontend/               # Turborepo
│   ├── apps/
│   │   ├── mobile/              # Expo Router (React Native)
│   │   └── web/                 # Next.js 14 App Router
│   └── packages/
│       ├── ui/                  # Componentes compartilhados (CycleDayCard, QuickLogButton, etc.)
│       ├── core/                # Lógica de domínio pura (TS)
│       ├── api/                 # Cliente Axios tipado + interceptors (refresh, encryption)
│       ├── utils/               # Encryption (Web Crypto), date, cycle math, validation
│       ├── theme/               # Tokens de design (cores, espaçamento, tipografia)
│       └── config/              # ESLint, Prettier, TypeScript, Tailwind, Jest presets
├── iara-infra/
│   ├── docker/
│   │   └── docker-compose.yml   # Postgres, Redis, Keycloak, MinIO, Backend, Frontend
│   ├── k8s/                     # Manifests base + overlays (dev/staging/prod)
│   └── scripts/
│       └── dev-start.sh         # Sobe tudo localmente (Pop!_OS / Linux ready)
└── iara-docs/
    ├── architecture/            # ADRs, diagramas C4
    ├── api/                     # OpenAPI specs por módulo
    ├── db/                      # Dicionário de dados, migrações
    ├── security/                # Threat model, crypto spec
    ├── privacy/                 # DPIA, data flow maps
    ├── ai-rules/                # Regras da camada de IA
    ├── partner-permissions/     # Matriz de permissões
    ├── roadmap/                 # MVP → V5
    └── testing/                 # Estratégia, casos de teste críticos
```

---

## 🚀 Como Rodar Localmente (Pop!_OS / Ubuntu / VS Code)

### Pré-requisitos
```bash
# Já vem no Pop!_OS ou instale:
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git openjdk-21-jdk maven nodejs pnpm gh
```

### 1. Clone e suba a infra
```bash
git clone [https://github.com/Lucas211194/iara-app.git](https://github.com/Lucas211194/iara-app.git)
cd iara-app
chmod +x iara-infra/scripts/dev-start.sh
./iara-infra/scripts/dev-start.sh
```
> O script sobe: **PostgreSQL (5432), Redis (6379), Keycloak (8080), MinIO (9000/9001), Backend (8081)** e roda migrações Flyway.

### 2. Backend (terminal separado)
```bash
cd iara-backend
# Compila e instala common primeiro
mvn install -pl iara-common -am -DskipTests

# Sobe módulos principais (auth, user, cycle, symptom, mood, sleep, weight, activity, reproductive, partner, alert, ai)
mvn spring-boot:run -pl iara-auth,iara-user,iara-cycle,iara-symptom,iara-mood,iara-sleep,iara-weight,iara-activity,iara-reproductive,iara-partner,iara-alert,iara-ai -am
```
- API: `http://localhost:8081/api/v1`
- Swagger (por módulo): `http://localhost:8081/api/v1/swagger-ui.html` (se habilitado no profile dev)

### 3. Frontend Web (terminal separado)
```bash
cd iara-frontend/apps/web
pnpm install
pnpm dev
```
- Web: `http://localhost:3000`

### 4. Mobile (terminal separado)
```bash
cd iara-frontend/apps/mobile
pnpm install
pnpm expo start --dev-client
```
- Abra no **Expo Go** (apenas JS) ou build **Dev Client** para testar biometria/encryption nativa:
  ```bash
  pnpm expo run:android   # ou :ios
  ```

---

## 🔑 Configuração de Secrets (`.env` local)

**NUNCA commite arquivos `.env` reais.** Use `.env.example` como modelo.

```bash
# iara-backend/.env (exemplo)
POSTGRES_PASSWORD=senha_forte_local
JWT_SECRET=chave_hmac_256_bits_base64_gerada_com_openssl
COLUMN_ENCRYPTION_KEY=chave_aes_256_base64_32_bytes
KEYCLOAK_ADMIN_PASSWORD=admin
MINIO_PASSWORD=minio_senha_forte
```

```bash
# iara-frontend/apps/web/.env.local
NEXT_PUBLIC_API_URL=http://localhost:8081/api/v1
NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8080
NEXT_PUBLIC_KEYCLOAK_REALM=iara
NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=iara-web
```

---

## ✅ Testes Críticos (Rodam no CI)

```bash
cd iara-backend

# 1. Arquitetura: módulos isolados, nada de analytics em domínio, IA não diagnostica
mvn test -Dtest=ArchitectureRulesTest

# 2. Guarda de privacidade: bloqueia dado sensível em Firebase/Amplitude/Sentry
mvn test -Dtest=ThirdPartyAnalyticsGuardTest

# 3. Previsão sempre como estimativa + regra dos 3 ciclos
mvn test -Dtest=CyclePredictionServiceTest

# 4. Permissão de parceiro bloqueia leitura NO BACKEND (não só UI)
mvn test -Dtest=PermissionGateServiceTest

# 5. Suite completa
mvn verify
```

### Frontend
```bash
cd iara-frontend
pnpm test           # Jest + React Testing Library
pnpm test:e2e       # Playwright (web) / Detox (mobile)
```

---

## 🗺️ Roadmap

| Versão | Entregável | Status |
|--------|------------|--------|
| **MVP** | Cadastro anônimo + Ciclo + Calendário + Sintomas básicos | 🚧 Em desenvolvimento |
| **V1** | Gráficos, comparação mensal, Dashboard 1-min, Modo sem identidade | ⏳ Planejado |
| **V2** | Conta parceiro (permissões graduais), Relatório para consulta médica, Conteúdo educativo (Febrasgo/OMS/MS) | 📋 Backlog |
| **V3** | Detecção de padrões pessoais (mín. 3 ciclos) + Alertas de saúde | 📋 Backlog |
| **V4** | Camada IA explicativa completa + Guardas clínicas | 📋 Backlog |
| **V5** | Health Connect / HealthKit (opt-in, dado nunca sai do device sem consentimento) | 📋 Backlog |

---

## 📄 Licença

MIT License — veja [LICENSE](LICENSE).

---

## 🤝 Contribuição

1. Fork → `git checkout -b feat/minha-feature`
2. Respeite as **regras de arquitetura** (ArchUnit passa)
3. Adicione testes para novas regras de privacidade
4. `mvn verify` + `pnpm test` passam
5. Abra PR com descrição clara do impacto em privacidade/segurança

---

## 📞 Contato / Suporte

- **Issues:** [GitHub Issues](https://github.com/Lucas211194/iara-app/issues) (bugs, propostas, segurança)
- **Security:** `security@iara.app` (ou abra *Security Advisory* privado no GitHub)
- **Maintainer:** [@Lucas211194](https://github.com/Lucas211194)

---

> **Lembrete:** Cada linha de código neste projeto deve honrar a promessa à usuária: *ela é a dona dos dados, o app só explica o que ela mesma registrou.*