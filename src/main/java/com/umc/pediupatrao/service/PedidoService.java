package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.enums.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Pedido criarPedido(Pedido pedido) {
        List<ItemPedido> itensValidos = new ArrayList<>();
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                if (item == null) {
                    continue;
                }
                boolean temDescricao = item.getNomeProduto() != null && !item.getNomeProduto().isBlank();
                boolean temQuantidade = item.getQuantidade() > 0;
                boolean temPreco = item.getPrecoUnitario() != null && item.getPrecoUnitario() > 0;
                if (temDescricao && temQuantidade && temPreco) {
                    item.setNomeProduto(item.getNomeProduto().trim());
                    itensValidos.add(item);
                }
            }
        }

        if (itensValidos.isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter ao menos um item.");
        }

        pedido.setItens(itensValidos);

        if (pedido.getStatus() == null) {
            pedido.setStatus(StatusPedido.RECEBIDO);
        }

        double valorBruto = 0.0;
        int quantidadeTotal = 0;
        for (ItemPedido item : itensValidos) {
            valorBruto += item.getQuantidade() * item.getPrecoUnitario();
            quantidadeTotal += item.getQuantidade();
        }

        pedido.setQuantidade(quantidadeTotal);
        if (pedido.getDesconto() == null) {
            pedido.setDesconto(0.0);
        }
        pedido.setDesconto(arredondarMonetario(pedido.getDesconto()));
        pedido.setValorTotal(arredondarMonetario(valorBruto - pedido.getDesconto()));

        ItemPedido primeiro = itensValidos.get(0);
        if (pedido.getTipoPizza() == null || pedido.getTipoPizza().isBlank()) {
            pedido.setTipoPizza(primeiro.getNomeProduto());
        }
        if (pedido.getSabor() == null || pedido.getSabor().isBlank()) {
            pedido.setSabor(primeiro.getNomeProduto());
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "INCLUSÃO DE PEDIDO",
                "Pedido",
                salvo.getId(),
                "pedido",
                null,
                resumoPedido(salvo),
                null
        );
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, String status) {
        return avancarStatus(id, status);
    }

    public Pedido avancarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        String statusAnterior = pedido.getStatus() != null ? pedido.getStatus().name() : null;
        pedido.setStatus(StatusPedido.valueOf(status));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "ALTERAÇÃO DE STATUS",
                "Pedido",
                salvo.getId(),
                "status",
                statusAnterior,
                salvo.getStatus().name(),
                null
        );
        return salvo;
    }

    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual != StatusPedido.RECEBIDO
                && statusAtual != StatusPedido.EM_PREPARACAO
                && statusAtual != StatusPedido.PRONTO) {
            throw new IllegalStateException(
                    "O pedido não pode mais ser cancelado, pois já saiu para entrega ou foi finalizado/cancelado.");
        }

        String statusAnterior = statusAtual != null ? statusAtual.name() : null;
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "CANCELAMENTO DE PEDIDO",
                "Pedido",
                salvo.getId(),
                "status",
                statusAnterior,
                "CANCELADO",
                justificativa
        );
        return salvo;
    }

    public Pedido aplicarDesconto(String id, Double percentual) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        if (percentual == null || percentual < 0 || percentual > 20.0) {
            throw new IllegalArgumentException("Desconto inválido. O máximo permitido é 20%.");
        }

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual != StatusPedido.RECEBIDO
                && statusAtual != StatusPedido.EM_PREPARACAO
                && statusAtual != StatusPedido.PRONTO) {
            throw new IllegalStateException("Desconto não pode ser aplicado após a saída do pedido.");
        }

        double valorBruto = 0.0;
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                valorBruto += item.getQuantidade() * item.getPrecoUnitario();
            }
        }

        String descontoAnterior = pedido.getDesconto() != null
                ? String.format(Locale.US, "%.2f", pedido.getDesconto())
                : "0.00";
        double desconto = arredondarMonetario(valorBruto * (percentual / 100.0));
        pedido.setDesconto(desconto);
        pedido.setValorTotal(arredondarMonetario(valorBruto - desconto));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "APLICAÇÃO DE DESCONTO",
                "Pedido",
                salvo.getId(),
                "desconto",
                descontoAnterior,
                String.format(Locale.US, "%.2f", salvo.getDesconto()),
                null
        );
        return salvo;
    }

    private double arredondarMonetario(double valor) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", valor));
    }

    private String resumoPedido(Pedido pedido) {
        String status = pedido.getStatus() != null ? pedido.getStatus().name() : "";
        String cliente = pedido.getClienteId() != null ? pedido.getClienteId() : "";
        return "status=" + status + ", clienteId=" + cliente;
    }

}
