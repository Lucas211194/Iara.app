#!/bin/bash
# Script para iniciar ambiente de dev no Pop!_OS
# Uso: chmod +x dev-start.sh && ./dev-start.sh

set -e

echo "🚀 Iniciando ambiente de desenvolvimento IARA..."

# 1. Verifica dependências
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ $1 não encontrado. Instale com: $2"
        exit 1
    fi
}

check_command docker "sudo apt install docker.io docker-compose"
check_command java "sudo apt install openjdk-21-jdk"
check_command mvn "sudo apt install maven"
check_command node "curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt install nodejs"
check_command pnpm "npm install -g pnpm"

# 2. Sobe infraestrutura
echo "📦 Subindo PostgreSQL, Redis, Keycloak, MinIO..."
cd "$(dirname "$0")/../docker"
docker-compose up -d postgres redis keycloak minio

# 3. Aguarda saúde
echo "⏳ Aguardando serviços ficarem saudáveis..."
sleep 10

# 4. Roda migrações Flyway (backend)
echo "🗄️ Rodando migrações de banco..."
cd ../../iara-backend
mvn flyway:migrate -pl iara-common,iara-auth,iara-user,iara-cycle,iara-symptom,iara-mood,iara-sleep,iara-weight,iara-activity,iara-reproductive,iara-partner,iara-alert,iara-ai -DskipTests

# 5. Instala dependências frontend
echo "📦 Instalando dependências frontend..."
cd ../../iara-frontend
pnpm install

# 6. Build packages compartilhados
echo "🔨 Building shared packages..."
pnpm --filter @iara/ui build
pnpm --filter @iara/core build
pnpm --filter @iara/api build

echo "✅ Ambiente pronto!"
echo ""
echo "Para iniciar:"
echo "  Backend:     cd iara-backend && mvn spring-boot:run -pl iara-auth,iara-user,iara-cycle,..."
echo "  Frontend Web: cd iara-frontend/apps/web && pnpm dev"
echo "  Mobile:      cd iara-frontend/apps/mobile && pnpm expo start"
echo ""
echo "URLs:"
echo "  API:         http://localhost:8081/api/v1"
echo "  Keycloak:    http://localhost:8080 (admin/admin)"
echo "  MinIO:       http://localhost:9001 (iara/iara_minio)"
echo "  Web:         http://localhost:3000"
echo "  Metro:       http://localhost:8081"