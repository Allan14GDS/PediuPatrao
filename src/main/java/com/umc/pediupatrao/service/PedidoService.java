package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.enums.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    public Pedido criarPedido(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(StatusPedido.valueOf(status));
        return pedidoRepository.save(pedido);
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

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa);
        return pedidoRepository.save(pedido);
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

        double desconto = valorBruto * (percentual / 100.0);
        pedido.setDesconto(desconto);
        pedido.setValorTotal(valorBruto - desconto);
        return pedidoRepository.save(pedido);
    }

}
