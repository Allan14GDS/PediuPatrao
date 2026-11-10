package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDadosDTO {

    private BigDecimal receitaTotal;
    private long totalPedidos;
    private BigDecimal ticketMedio;
    private double taxaCancelamento;
    private Map<String, BigDecimal> evolucaoVendas = new LinkedHashMap<>();
    private Map<String, Long> situacaoPedidos = new LinkedHashMap<>();
    private List<ProdutoRankingDTO> ranking = new ArrayList<>();

    public BigDecimal getReceitaTotal() {
        return receitaTotal;
    }

    public void setReceitaTotal(BigDecimal receitaTotal) {
        this.receitaTotal = receitaTotal;
    }

    public long getTotalPedidos() {
        return totalPedidos;
    }

    public void setTotalPedidos(long totalPedidos) {
        this.totalPedidos = totalPedidos;
    }

    public BigDecimal getTicketMedio() {
        return ticketMedio;
    }

    public void setTicketMedio(BigDecimal ticketMedio) {
        this.ticketMedio = ticketMedio;
    }

    public double getTaxaCancelamento() {
        return taxaCancelamento;
    }

    public void setTaxaCancelamento(double taxaCancelamento) {
        this.taxaCancelamento = taxaCancelamento;
    }

    public Map<String, BigDecimal> getEvolucaoVendas() {
        return evolucaoVendas;
    }

    public void setEvolucaoVendas(Map<String, BigDecimal> evolucaoVendas) {
        this.evolucaoVendas = evolucaoVendas;
    }

    public Map<String, Long> getSituacaoPedidos() {
        return situacaoPedidos;
    }

    public void setSituacaoPedidos(Map<String, Long> situacaoPedidos) {
        this.situacaoPedidos = situacaoPedidos;
    }

    public List<ProdutoRankingDTO> getRanking() {
        return ranking;
    }

    public void setRanking(List<ProdutoRankingDTO> ranking) {
        this.ranking = ranking;
    }
}
