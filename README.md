# MotoTrack

API REST para cadastro, acompanhamento e consulta de viagens de motocicleta.
O projeto usa Java 25, Spring Boot, Spring Data JPA e PostgreSQL.

## Executando localmente

Pré-requisitos: Docker e uma JDK 25 configurada.

```bash
docker compose up -d
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8080`. O PostgreSQL é publicado na
porta `5432`. Os valores padrão permitem iniciar o projeto sem configuração
adicional.

Para personalizar as credenciais, copie o arquivo de exemplo e exporte as
variáveis antes de iniciar a aplicação:

```bash
cp .env.example .env
set -a
source .env
set +a
./mvnw spring-boot:run
```

O arquivo `.env` não é versionado. As variáveis aceitas pela aplicação são
`DB_URL`, `DB_USER` e `DB_PASSWORD`; o Docker Compose usa `POSTGRES_DB`,
`POSTGRES_USER` e `POSTGRES_PASSWORD`.

## Endpoints principais

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/trips` | Agenda uma viagem |
| `POST` | `/trips/completed` | Registra uma viagem já concluída |
| `GET` | `/trips` | Lista viagens; aceita `?status=PLANNED` |
| `GET` | `/trips/{id}` | Busca uma viagem por ID |
| `GET` | `/trips/upcoming` | Lista próximas viagens planejadas |
| `GET` | `/trips/terrain/{terrain}` | Filtra por `ASPHALT`, `MIXED` ou `OFF_ROAD` |
| `GET` | `/trips/date/{date}` | Filtra pela data no formato `AAAA-MM-DD` |
| `GET` | `/trips/{id}/days-until` | Calcula os dias até uma viagem planejada |
| `PATCH` | `/trips/{id}/status` | Altera o status da viagem |
| `GET` | `/statistics` | Retorna o resumo estatístico das viagens |

Exemplo de viagem planejada:

```bash
curl -X POST http://localhost:8080/trips \
  -H 'Content-Type: application/json' \
  -d '{
    "origin": "Florianopolis",
    "destination": "Urubici",
    "distanceKm": 175.5,
    "terrain": "MIXED",
    "tripDate": "2030-09-20",
    "motorcycle": {
      "id": 1,
      "brand": "Honda",
      "model": "NX 500",
      "year": 2025,
      "engineCapacity": 471
    }
  }'
```

O ID da viagem é gerado pelo banco. A estatística `mostUsedMotorcycle` considera
somente viagens com status `COMPLETED`.

## Testes

```bash
./mvnw test
```

Os testes do adapter JPA usam um banco H2 descartável e rollback por teste. O
PostgreSQL de desenvolvimento não é alterado pela suíte automatizada.

Para encerrar o banco local:

```bash
docker compose down
```

Para também remover os dados persistidos no volume:

```bash
docker compose down -v
```
