# API MotoTrack

Contrato HTTP atual para o cliente Android. Os exemplos usam `http://localhost:8080`
apenas para desenvolvimento local; use HTTPS quando a API estiver acessível pela rede.

## Convenções

- Corpo de request e response: JSON (`Content-Type: application/json` quando houver corpo).
- Datas: `yyyy-MM-dd`, sem horário. As regras de “hoje” usam
  `America/Sao_Paulo`, independentemente do timezone do servidor ou do aparelho.
- `TerrainType`: `ASPHALT`, `MIXED`, `OFF_ROAD`.
- `TripStatus`: `PLANNED`, `IN_PROGRESS`, `COMPLETED`.
- Identificadores são números inteiros positivos nos requests que os validam.
- `POST /users` é público. Todos os demais endpoints exigem HTTP Basic com
  e-mail e senha em cada requisição. O e-mail é normalizado (espaços nas pontas
  removidos e letras minúsculas); a senha não é retornada pela API.
- O proprietário é sempre obtido da autenticação, nunca de um `ownerId` no
  request. Recursos existentes de outro usuário retornam `403`; IDs inexistentes
  retornam `404`. Listagens e estatísticas incluem apenas dados do usuário.

```bash
curl -u marcos@example.com:secret123 http://localhost:8080/users/me
```

## Formatos de resposta

| Nome | Campos |
|---|---|
| `User` | `id`, `name`, `email` |
| `Motorcycle` | `id`, `brand`, `model`, `color`, `year`, `engineCapacity`, `owner: User` |
| `Trip` | `id`, `origin`, `destination`, `distanceKm`, `status`, `terrain`, `tripDate`, `motorcycle: Motorcycle` |
| `TripSummary` | `id`, `origin`, `destination`, `distanceKm`, `terrain`, `tripDate`, `motorcycle: Motorcycle` |
| `MotorcycleTripSummary` | `id`, `origin`, `destination`, `distanceKm`, `terrain`, `tripDate` (sem `motorcycle` e sem `status`) |
| `TerrainStatistics` | `tripCount`, `totalDistanceKm` |
| `MonthlyTripStatistics` | `year`, `month` (1–12), `tripCount`, `totalDistanceKm` |

Listas vazias são `[]`. Objetos opcionais de home/estatísticas são `null` quando
não há viagem correspondente. A média de distância é `null` quando a contagem
de viagens concluídas é zero. Mapas de status/terreno retornam todas as chaves
dos enums, com zero para categorias sem viagens.

## Usuários

### `POST /users` — cadastrar usuário

Público. Request: `name` (não vazio), `email` (válido, não vazio) e `password`
(não vazia, mínimo de 8 caracteres). O e-mail é salvo em minúsculas. Retorna
`201` com `User`, sem senha/hash. Erros: `400` por validação/JSON inválido,
`409` para e-mail já cadastrado.

```bash
curl -X POST http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Marcos","email":"marcos@example.com","password":"secret123"}'
```

```json
{"id":1,"name":"Marcos","email":"marcos@example.com"}
```

### `GET /users/me` — usuário autenticado

Sem parâmetros ou corpo. Retorna `200` com `User`; `401` sem credenciais válidas.

## Motocicletas

| Método e rota | Parâmetros / request | Sucesso | Outros status relevantes |
|---|---|---|---|
| `POST /motorcycles` | JSON: `brand`, `model`, `color` não vazios; `year >= 1900`; `engineCapacity > 0`. Não aceita owner escolhido pelo cliente. | `201` + `Motorcycle` | `400` |
| `GET /motorcycles` | Sem parâmetros. | `200` + `Motorcycle[]` | — |
| `GET /motorcycles/{id}` | `id`: ID da moto. | `200` + `Motorcycle` | `400` ID inválido; `403` outro owner; `404` inexistente |
| `DELETE /motorcycles/{id}` | `id`: ID da moto; sem corpo. | `204` sem corpo | `400` ID inválido; `403` outro owner; `404` inexistente; `409` moto com qualquer viagem vinculada |

Exemplo de criação:

```bash
curl -u marcos@example.com:secret123 -X POST http://localhost:8080/motorcycles \
  -H 'Content-Type: application/json' \
  -d '{"brand":"Honda","model":"NX 500","color":"Black","year":2025,"engineCapacity":471}'
```

```json
{
  "id": 1, "brand": "Honda", "model": "NX 500", "color": "Black",
  "year": 2025, "engineCapacity": 471,
  "owner": {"id": 1, "name": "Marcos", "email": "marcos@example.com"}
}
```

### `GET /motorcycles/{motorcycleId}/statistics`

`motorcycleId`: ID da moto. Retorna `200` com:

- `motorcycle: Motorcycle`;
- `completedTrips: number` e `totalCompletedDistanceKm: number`;
- `averageCompletedDistanceKm: number | null`;
- `longestTrip` e `lastCompletedTrip`: `MotorcycleTripSummary | null`;
- `terrainStatistics`: mapa `ASPHALT`/`MIXED`/`OFF_ROAD` para
  `TerrainStatistics`, sempre incluindo terrenos zerados.

Somente viagens `COMPLETED` dessa moto entram nos cálculos. Maior viagem:
`distanceKm DESC`, depois `tripDate DESC`, depois `id DESC`. Última viagem:
`tripDate DESC`, depois `id DESC`. Uma moto recém-cadastrada retorna contagem e
distância zero, média e destaques `null`. Erros: `400` para ID malformado, `403`
para moto de outro usuário, `404` para moto inexistente.

```bash
curl -u marcos@example.com:secret123 \
  http://localhost:8080/motorcycles/1/statistics
```

```json
{
  "motorcycle": {
    "id": 1, "brand": "Honda", "model": "NX 500", "color": "Black",
    "year": 2025, "engineCapacity": 471,
    "owner": {"id": 1, "name": "Marcos", "email": "marcos@example.com"}
  },
  "completedTrips": 2,
  "totalCompletedDistanceKm": 500.0,
  "averageCompletedDistanceKm": 250.0,
  "longestTrip": {
    "id": 7, "origin": "Florianopolis", "destination": "Urubici",
    "distanceKm": 300.0, "terrain": "MIXED", "tripDate": "2026-09-12"
  },
  "lastCompletedTrip": {
    "id": 7, "origin": "Florianopolis", "destination": "Urubici",
    "distanceKm": 300.0, "terrain": "MIXED", "tripDate": "2026-09-12"
  },
  "terrainStatistics": {
    "ASPHALT": {"tripCount": 1, "totalDistanceKm": 200.0},
    "MIXED": {"tripCount": 1, "totalDistanceKm": 300.0},
    "OFF_ROAD": {"tripCount": 0, "totalDistanceKm": 0.0}
  }
}
```

## Viagens

Para `POST /trips` e `POST /trips/completed`, o request é o mesmo:
`origin` e `destination` não vazios, `distanceKm > 0`, `terrain` válido,
`tripDate` ISO e `motorcycleId > 0` de uma moto própria. Nenhum dos dois
requests aceita `status` ou owner escolhidos pelo cliente.

```json
{
  "origin": "Florianopolis", "destination": "Urubici",
  "distanceKm": 175.5, "terrain": "MIXED",
  "tripDate": "2026-10-15", "motorcycleId": 1
}
```

| Método e rota | Parâmetros / regra | Sucesso | Outros status relevantes |
|---|---|---|---|
| `POST /trips` | JSON acima; `tripDate` não pode ser anterior a hoje. Cria `PLANNED`. | `201` + `Trip` | `400` payload/data inválida; `403` moto alheia; `404` moto inexistente |
| `POST /trips/completed` | Mesmo JSON; `tripDate` não pode ser posterior a hoje. Cria `COMPLETED`. | `201` + `Trip` | `400`, `403`, `404` como acima |
| `GET /trips` | `status` opcional: `PLANNED`, `IN_PROGRESS`, `COMPLETED`. Sem filtro: todas as viagens próprias. Para `COMPLETED`, ordem `tripDate DESC, id DESC`; nas demais listagens a ordem não é garantida. | `200` + `Trip[]` | `400` status inválido |
| `GET /trips/{id}` | `id`: ID da viagem. | `200` + `Trip` | `400` ID inválido; `403` outro owner; `404` inexistente |
| `GET /trips/upcoming` | Viagens `PLANNED` com `tripDate >= hoje`, ordenadas por data crescente. | `200` + `Trip[]` | — |
| `GET /trips/terrain/{terrain}` | `terrain`: `ASPHALT`, `MIXED` ou `OFF_ROAD`; inclui todos os status. | `200` + `Trip[]` | `400` terreno inválido |
| `GET /trips/date/{date}` | `date`: data `yyyy-MM-dd`; inclui todos os status. | `200` + `Trip[]` | `400` data inválida |
| `GET /trips/{id}/days-until` | Apenas `PLANNED`; `id` da viagem. Viagens planejadas vencidas podem retornar número negativo. | `200` + número inteiro JSON (ex.: `3`) | `400` status/ID inválido; `403` outro owner; `404` inexistente |
| `PATCH /trips/{id}/status` | JSON `{ "status": "IN_PROGRESS" }` ou `{ "status": "COMPLETED" }`. Só `PLANNED → IN_PROGRESS → COMPLETED`; concluir requer `tripDate <= hoje`. | `204` sem corpo | `400` transição/data/payload/ID inválidos; `403` outro owner; `404` inexistente |
| `DELETE /trips/{id}` | `id` da viagem; sem corpo. | `204` sem corpo | `400` ID inválido; `403` outro owner; `404` inexistente |

Exemplo de resposta `Trip` de uma viagem planejada:

```json
{
  "id": 5, "origin": "Florianopolis", "destination": "Urubici",
  "distanceKm": 175.5, "status": "PLANNED", "terrain": "MIXED",
  "tripDate": "2026-10-15",
  "motorcycle": {
    "id": 1, "brand": "Honda", "model": "NX 500", "color": "Black",
    "year": 2025, "engineCapacity": 471,
    "owner": {"id": 1, "name": "Marcos", "email": "marcos@example.com"}
  }
}
```

## Home e estatísticas gerais

### `GET /home`

Sem parâmetros. Retorna `200` com `nextTrip: NextTrip | null`,
`lastCompletedTrip: TripSummary | null`, `totalCompletedDistanceKm: number`,
`completedTrips: number`, `motorcycleCount: number`.

`NextTrip` tem `id`, `origin`, `destination`, `distanceKm`, `terrain`,
`tripDate`, `daysUntil` e `motorcycle: Motorcycle`. É a primeira viagem futura
`PLANNED` por data. Se não houver, `nextTrip` é `null`. Apenas viagens
`COMPLETED` entram em `lastCompletedTrip`, distância e contagem.

```json
{
  "nextTrip": null,
  "lastCompletedTrip": null,
  "totalCompletedDistanceKm": 0.0,
  "completedTrips": 0,
  "motorcycleCount": 1
}
```

### `GET /statistics`

Sem parâmetros. Retorna `200` com:

| Campo | Significado |
|---|---|
| `totalCompletedTrips` | Número de viagens concluídas. |
| `totalCompletedDistance` | Soma dos km concluídos, arredondada para uma casa decimal nesta resposta. |
| `averageCompletedDistanceKm` | Média por viagem concluída; `null` se não houver. |
| `mostUsedMotorcycle` | `Motorcycle` com mais viagens concluídas, ou `null`. Empate entre motos não possui desempate garantido. |
| `longestTrip`, `firstCompletedTrip`, `lastCompletedTrip` | `TripSummary` ou `null`. Maior: km DESC, data DESC, ID DESC. Primeira/última: data e ID ascendentes/descendentes. |
| `tripsByStatus` | Contagem de `PLANNED`, `IN_PROGRESS`, `COMPLETED`; chaves sempre presentes. |
| `terrainStatistics` | Km e contagem de viagens concluídas por terreno; chaves sempre presentes. |
| `monthlyStatistics` | Exatamente 12 itens `MonthlyTripStatistics`, do mês de 11 meses atrás ao atual, em ordem cronológica; meses vazios zerados. |

Exemplo para uma conta sem viagens em setembro de 2026:

```json
{
  "totalCompletedTrips": 0,
  "totalCompletedDistance": 0.0,
  "averageCompletedDistanceKm": null,
  "mostUsedMotorcycle": null,
  "longestTrip": null,
  "firstCompletedTrip": null,
  "lastCompletedTrip": null,
  "tripsByStatus": {"PLANNED": 0, "IN_PROGRESS": 0, "COMPLETED": 0},
  "terrainStatistics": {
    "ASPHALT": {"tripCount": 0, "totalDistanceKm": 0.0},
    "MIXED": {"tripCount": 0, "totalDistanceKm": 0.0},
    "OFF_ROAD": {"tripCount": 0, "totalDistanceKm": 0.0}
  },
  "monthlyStatistics": [
    {"year": 2025, "month": 10, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2025, "month": 11, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2025, "month": 12, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 1, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 2, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 3, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 4, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 5, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 6, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 7, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 8, "tripCount": 0, "totalDistanceKm": 0.0},
    {"year": 2026, "month": 9, "tripCount": 0, "totalDistanceKm": 0.0}
  ]
}
```

Não há parâmetros de filtro em `/home` ou `/statistics`.

## Erros HTTP

Todos os erros tratados pela API usam o mesmo corpo. `errors` é um objeto
vazio exceto em falhas de Bean Validation, quando mapeia nome do campo para
uma mensagem. O `401` também inclui `WWW-Authenticate: Basic realm="Realm"`.

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "A requisição contém campos inválidos.",
  "errors": {"distanceKm": "A distância deve ser maior que zero."}
}
```

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Você não possui permissão para acessar esta motocicleta.",
  "errors": {}
}
```

| HTTP | Situação |
|---|---|
| `400 Bad Request` | Regra de data/status inválida, validação, JSON inválido, valor inválido em parâmetro de URL. JSON malformado recebe mensagem genérica, sem detalhes internos. |
| `401 Unauthorized` | Credenciais ausentes ou incorretas. |
| `403 Forbidden` | Recurso pertence a outro usuário ou acesso negado pela segurança. |
| `404 Not Found` | Usuário/recurso inexistente ou endpoint não encontrado. |
| `409 Conflict` | E-mail duplicado ou moto ainda vinculada a viagens. |
| `405 Method Not Allowed` | Método HTTP não disponível na rota. O cabeçalho `Allow` é preservado. |
| `406 Not Acceptable` | Formato solicitado no `Accept` não suportado. |
| `415 Unsupported Media Type` | `Content-Type` do request não suportado. |

O campo `error` mantém o motivo HTTP em inglês (`Bad Request`, `Unauthorized`,
`Forbidden`, `Not Found`, `Conflict`), exceto a validação, que preserva
`Validation Failed`. O campo `message` é legível para o usuário; o cliente
deve usar `status` para a lógica de controle, não comparar mensagens.
