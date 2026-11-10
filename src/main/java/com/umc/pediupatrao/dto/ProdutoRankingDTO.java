package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public class ProdutoRankingDTO {

    private String nome;
    private long quantidadeVendida;
    private BigDecimal receitaGerada;

    public ProdutoRankingDTO() {
    }

    public ProdutoRankingDTO(String nome, long quantidadeVendida, BigDecimal receitaGerada) {
        this.nome = nome;
        this.quantidadeVendida = quantidadeVendida;
        this.receitaGerada = receitaGerada;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public long getQuantidadeVendida() {
        return quantidadeVendida;
    }

    public void setQuantidadeVendida(long quantidadeVendida) {
        this.quantidadeVendida = quantidadeVendida;
    }

    public BigDecimal getReceitaGerada() {
        return receitaGerada;
    }

    public void setReceitaGerada(BigDecimal receitaGerada) {
        this.receitaGerada = receitaGerada;
    }
}
