# Documentação - Dashboard Administrativo "Pediu Patrão"

## 1. Requisitos do Sistema

### Requisitos Funcionais (RF)
* **RF01 — Indicadores gerais:** Exibição de Receita Total, Total de Pedidos, Ticket Médio e Taxa de Cancelamento.
* **RF02 — Análise de vendas:** Gráfico de evolução de receita baseado no período selecionado (Chart.js).
* **RF03 — Situação dos pedidos:** Gráfico de distribuição do status do fluxo operacional.
* **RF04 — Produtos mais vendidos:** Tabela com o Top 5 produtos ranqueados por quantidade e receita.
* **RF05 — Filtros de consulta:** Seletor de período dinâmico (Hoje, 7 dias, 30 dias, Todo o período) que atualiza os dados em tempo real.
* **RF06 e RF07 — Integração e Tratamento:** Painel consome via `Fetch API` os dados mastigados e agregados pelo backend.

### Requisitos Não Funcionais (RNF)
* **Segurança:** As rotas da View (`/dashboard`) e da API (`/api/dashboard/**`) são protegidas pelo Spring Security (`hasAnyRole('GERENTE', 'ADMIN')`).
* **Performance:** Todo o processamento matemático, agrupamento e ordenação é delegado ao banco de dados utilizando o **Aggregation Framework do MongoDB** (via `MongoTemplate`), evitando sobrecarga de memória na aplicação Java.
* **Interface (UI/UX):** Design responsivo implementado com Tailwind CSS e ícones Lucide, seguindo os padrões visuais limpos de painéis SaaS modernos.

---

## 2. Documentação da API

### `GET /api/dashboard/dados`
Retorna todos os indicadores, dados para gráficos e ranking de produtos consolidados com base no período informado.

**Parâmetros de Requisição (Query Params):**
* `periodo` (String, Opcional) - Define o intervalo de busca. Valores aceitos: `hoje`, `7d`, `30d`, `todos`. Padrão: `7d`.

**Exemplo de Requisição:**
`GET /api/dashboard/dados?periodo=30d`

**Exemplo de Resposta de Sucesso (200 OK):**
```json
{
  "receitaTotal": 371.41,
  "totalPedidos": 6,
  "ticketMedio": 92.85,
  "taxaCancelamento": 33.3,
  "evolucaoVendas": {
    "2026-11-09": 150.00,
    "2026-11-10": 221.41
  },
  "situacaoPedidos": {
    "FINALIZADO": 3,
    "CANCELADO": 2,
    "RECEBIDO": 1
  },
  "ranking": [
    {
      "nome": "Pizza Calabresa",
      "quantidadeVendida": 4,
      "receitaGerada": 180.00
    }
  ]
}

Possíveis Erros mapeados:
403 Forbidden: Retornado caso um usuário sem permissão (ex: ATENDENTE) tente acessar o endpoint.
400 Bad Request: Retornado caso seja enviado um parâmetro de período inválido (diferente da lista permitida).

