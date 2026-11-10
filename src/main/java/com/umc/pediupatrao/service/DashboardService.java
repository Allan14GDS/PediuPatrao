package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDadosDTO;
import com.umc.pediupatrao.dto.ProdutoRankingDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.enums.StatusPedido;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private static final String STATUS_CANCELADO = StatusPedido.CANCELADO.name();
    private static final int LIMITE_RANKING = 5;

    private final MongoTemplate mongoTemplate;

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public DashboardDadosDTO obterDadosDashboard(String periodo) {
        Intervalo intervalo = intervalo(periodo);

        Document indicadores = agregarIndicadores(intervalo);
        long totalPedidos = paraLong(indicadores.get("totalPedidos"));
        long pedidosConsiderados = paraLong(indicadores.get("pedidosConsiderados"));
        long cancelados = paraLong(indicadores.get("cancelados"));
        BigDecimal receitaTotal = monetario(indicadores.get("receitaTotal"));

        DashboardDadosDTO dados = new DashboardDadosDTO();
        dados.setReceitaTotal(receitaTotal);
        dados.setTotalPedidos(totalPedidos);
        dados.setTicketMedio(ticketMedio(receitaTotal, pedidosConsiderados));
        dados.setTaxaCancelamento(taxaCancelamento(cancelados, totalPedidos));
        dados.setEvolucaoVendas(agregarEvolucao(intervalo));
        dados.setSituacaoPedidos(agregarSituacao(intervalo));
        dados.setRanking(agregarRanking(intervalo));
        return dados;
    }

    private Document agregarIndicadores(Intervalo intervalo) {
        ConditionalOperators.Cond receita = ConditionalOperators
                .when(ComparisonOperators.Ne.valueOf("status").notEqualToValue(STATUS_CANCELADO))
                .thenValueOf("valorTotal")
                .otherwise(0);
        ConditionalOperators.Cond considerados = ConditionalOperators
                .when(ComparisonOperators.Ne.valueOf("status").notEqualToValue(STATUS_CANCELADO))
                .then(1)
                .otherwise(0);
        ConditionalOperators.Cond cancelados = ConditionalOperators
                .when(ComparisonOperators.Eq.valueOf("status").equalToValue(STATUS_CANCELADO))
                .then(1)
                .otherwise(0);

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(filtroPeriodo(intervalo)),
                Aggregation.group()
                        .sum(receita).as("receitaTotal")
                        .count().as("totalPedidos")
                        .sum(considerados).as("pedidosConsiderados")
                        .sum(cancelados).as("cancelados")
        );

        Document resultado = mongoTemplate.aggregate(aggregation, Pedido.class, Document.class).getUniqueMappedResult();
        return resultado != null ? resultado : new Document();
    }

    private Map<String, BigDecimal> agregarEvolucao(Intervalo intervalo) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(vendasNoPeriodo(intervalo)),
                Aggregation.project()
                        .and(DateOperators.DateToString.dateOf("dataHoraEntrada")
                                .toString("%Y-%m-%d")
                                .withTimezone(DateOperators.Timezone.fromZone(ZoneId.systemDefault())))
                        .as("dia")
                        .and("valorTotal").as("valorTotal"),
                Aggregation.group("dia").sum("valorTotal").as("total"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );

        Map<String, BigDecimal> apurados = new LinkedHashMap<>();
        for (Document documento : agregar(aggregation)) {
            Object dia = documento.get("_id");
            if (dia == null) {
                continue;
            }
            apurados.put(dia.toString(), monetario(documento.get("total")));
        }
        return completarDias(intervalo, apurados);
    }

    private Map<String, Long> agregarSituacao(Intervalo intervalo) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(filtroPeriodo(intervalo)),
                Aggregation.group("status").count().as("quantidade")
        );

        Map<String, Long> situacao = new LinkedHashMap<>();
        for (StatusPedido status : StatusPedido.values()) {
            situacao.put(status.name(), 0L);
        }
        for (Document documento : agregar(aggregation)) {
            Object status = documento.get("_id");
            if (status == null) {
                continue;
            }
            situacao.put(status.toString(), paraLong(documento.get("quantidade")));
        }
        return situacao;
    }

    private List<ProdutoRankingDTO> agregarRanking(Intervalo intervalo) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(vendasNoPeriodo(intervalo)),
                Aggregation.unwind("itens"),
                Aggregation.match(Criteria.where("itens.nomeProduto").nin(null, "")),
                Aggregation.group("itens.nomeProduto")
                        .sum("itens.quantidade").as("quantidadeVendida")
                        .sum(ArithmeticOperators.Multiply.valueOf("itens.quantidade")
                                .multiplyBy("itens.precoUnitario"))
                        .as("receitaGerada"),
                Aggregation.sort(Sort.by(
                        Sort.Order.desc("quantidadeVendida"),
                        Sort.Order.desc("receitaGerada"))),
                Aggregation.limit(LIMITE_RANKING)
        );

        List<ProdutoRankingDTO> ranking = new ArrayList<>();
        for (Document documento : agregar(aggregation)) {
            Object nome = documento.get("_id");
            if (nome == null || nome.toString().isBlank()) {
                continue;
            }
            ranking.add(new ProdutoRankingDTO(
                    nome.toString(),
                    paraLong(documento.get("quantidadeVendida")),
                    monetario(documento.get("receitaGerada"))
            ));
        }
        return ranking;
    }

    private List<Document> agregar(Aggregation aggregation) {
        AggregationResults<Document> resultados = mongoTemplate.aggregate(aggregation, Pedido.class, Document.class);
        return resultados.getMappedResults();
    }

    private Criteria filtroPeriodo(Intervalo intervalo) {
        if (!intervalo.limitado()) {
            return new Criteria();
        }
        return Criteria.where("dataHoraEntrada").gte(intervalo.inicio()).lt(intervalo.fim());
    }

    private Criteria vendasNoPeriodo(Intervalo intervalo) {
        Criteria naoCancelado = Criteria.where("status").ne(STATUS_CANCELADO);
        if (!intervalo.limitado()) {
            return naoCancelado;
        }
        return new Criteria().andOperator(filtroPeriodo(intervalo), naoCancelado);
    }

    private Map<String, BigDecimal> completarDias(Intervalo intervalo, Map<String, BigDecimal> apurados) {
        if (!intervalo.limitado()) {
            return apurados;
        }
        Map<String, BigDecimal> serie = new LinkedHashMap<>();
        LocalDate dia = intervalo.inicio().toLocalDate();
        LocalDate ultimo = intervalo.fim().toLocalDate().minusDays(1);
        while (!dia.isAfter(ultimo)) {
            String chave = dia.toString();
            serie.put(chave, apurados.getOrDefault(chave, zero()));
            dia = dia.plusDays(1);
        }
        return serie;
    }

    private Intervalo intervalo(String periodo) {
        String chave = periodo == null ? "" : periodo.trim().toLowerCase(Locale.ROOT);
        LocalDate hoje = LocalDate.now();
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay();
        return switch (chave) {
            case "hoje" -> new Intervalo(hoje.atStartOfDay(), fim);
            case "7d" -> new Intervalo(hoje.minusDays(6).atStartOfDay(), fim);
            case "30d" -> new Intervalo(hoje.minusDays(29).atStartOfDay(), fim);
            case "todos" -> new Intervalo(null, null);
            default -> throw new IllegalArgumentException("Período inválido. Use hoje, 7d, 30d ou todos.");
        };
    }

    private BigDecimal ticketMedio(BigDecimal receitaTotal, long pedidosConsiderados) {
        if (pedidosConsiderados == 0) {
            return zero();
        }
        return receitaTotal.divide(BigDecimal.valueOf(pedidosConsiderados), 2, RoundingMode.HALF_UP);
    }

    private double taxaCancelamento(long cancelados, long totalPedidos) {
        if (totalPedidos == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(cancelados)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalPedidos), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal monetario(Object valor) {
        if (valor == null) {
            return zero();
        }
        BigDecimal numero;
        if (valor instanceof Decimal128 decimal) {
            numero = decimal.bigDecimalValue();
        } else if (valor instanceof BigDecimal decimal) {
            numero = decimal;
        } else if (valor instanceof Double || valor instanceof Float) {
            numero = BigDecimal.valueOf(((Number) valor).doubleValue());
        } else if (valor instanceof Number numeroValor) {
            numero = BigDecimal.valueOf(numeroValor.longValue());
        } else {
            numero = new BigDecimal(valor.toString());
        }
        return numero.setScale(2, RoundingMode.HALF_UP);
    }

    private long paraLong(Object valor) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        return 0L;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
        private boolean limitado() {
            return inicio != null && fim != null;
        }
    }
}
