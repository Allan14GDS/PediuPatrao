package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.enums.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    public Pedido criarPedido(Pedido pedido) {
        pedido.setStatus(StatusPedido.RECEBIDO);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido avancarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual == StatusPedido.CANCELADO || statusAtual == StatusPedido.FINALIZADO) {
            throw new IllegalStateException("Pedido não pode ter seu status alterado em estado terminal.");
        }

        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.setDataHoraSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(SecurityContextHolder.getContext().getAuthentication().getName());
        }

        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }

}
