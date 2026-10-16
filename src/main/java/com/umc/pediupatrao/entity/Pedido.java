package com.umc.pediupatrao.entity;

import com.umc.pediupatrao.enums.StatusPedido;
import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private StatusPedido status;
    private int quantidade;
    private String sabor;
    private String justificativaCancelamento;
    private List<ItemPedido> itens;
    private Double desconto;
    private Double valorTotal;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public String getJustificativaCancelamento() {
        return justificativaCancelamento;
    }

    public void setJustificativaCancelamento(String justificativaCancelamento) {
        this.justificativaCancelamento = justificativaCancelamento;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public Double getDesconto() {
        return desconto;
    }

    public void setDesconto(Double desconto) {
        this.desconto = desconto;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }
}
