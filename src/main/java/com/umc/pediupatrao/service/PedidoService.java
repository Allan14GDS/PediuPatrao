package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.enums.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        Double descontoInformado = pedido.getDesconto();
        pedido.setValorTotal(null);
        pedido.setDesconto(null);

        double valorBruto = 0.0;
        int quantidadeTotal = 0;
        for (ItemPedido item : itensValidos) {
            valorBruto += item.getQuantidade() * item.getPrecoUnitario();
            quantidadeTotal += item.getQuantidade();
        }
        valorBruto = arredondarMonetario(valorBruto);

        double desconto = descontoAplicavel(descontoInformado, valorBruto);
        pedido.setQuantidade(quantidadeTotal);
        pedido.setDesconto(desconto);
        pedido.setValorTotal(arredondarMonetario(valorBruto - desconto));
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setUsuarioResponsavel(usuarioAutenticado());

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
        StatusPedido atual = pedido.getStatus();
        if (atual == StatusPedido.FINALIZADO || atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido finalizado ou cancelado não pode ter o status alterado.");
        }

        StatusPedido proximo = interpretarStatus(status);
        if (!transicaoPermitida(atual, proximo)) {
            throw new IllegalStateException(
                    "Transição de status inválida. Não é permitido retroceder nem pular etapas.");
        }

        String statusAnterior = atual != null ? atual.name() : "(vazio)";
        pedido.setStatus(proximo);
        if (proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO) {
            pedido.setDataHoraSaida(LocalDateTime.now());
            pedido.setUsuarioResponsavelSaida(usuarioAutenticado());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "ALTERAÇÃO DE STATUS",
                "Pedido",
                salvo.getId(),
                Map.of("status", statusAnterior),
                Map.of("status", salvo.getStatus().name()),
                null
        );
        return salvo;
    }

    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();

        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa é obrigatória para cancelar o pedido.");
        }

        StatusPedido statusAtual = pedido.getStatus();
        if (statusAtual == StatusPedido.SAIU_PARA_ENTREGA || statusAtual == StatusPedido.RETIRADO) {
            throw new IllegalStateException(
                    "O pedido não pode mais ser cancelado, pois já saiu para entrega ou foi retirado.");
        }
        if (statusAtual != StatusPedido.RECEBIDO
                && statusAtual != StatusPedido.EM_PREPARACAO
                && statusAtual != StatusPedido.PRONTO) {
            throw new IllegalStateException(
                    "O pedido não pode mais ser cancelado, pois já foi finalizado ou cancelado.");
        }

        String statusAnterior = statusAtual != null ? statusAtual.name() : "(vazio)";
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(
                "CANCELAMENTO DE PEDIDO",
                "Pedido",
                salvo.getId(),
                Map.of("status", statusAnterior),
                Map.of("status", "CANCELADO"),
                justificativa.trim()
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
        String valorTotalAnterior = pedido.getValorTotal() != null
                ? String.format(Locale.US, "%.2f", pedido.getValorTotal())
                : "0.00";
        double desconto = arredondarMonetario(valorBruto * (percentual / 100.0));
        pedido.setDesconto(desconto);
        pedido.setValorTotal(arredondarMonetario(valorBruto - desconto));
        Pedido salvo = pedidoRepository.save(pedido);

        Map<String, String> valoresAnteriores = new LinkedHashMap<>();
        valoresAnteriores.put("desconto", descontoAnterior);
        valoresAnteriores.put("valorTotal", valorTotalAnterior);
        Map<String, String> valoresNovos = new LinkedHashMap<>();
        valoresNovos.put("desconto", String.format(Locale.US, "%.2f", salvo.getDesconto()));
        valoresNovos.put("valorTotal", String.format(Locale.US, "%.2f", salvo.getValorTotal()));
        auditoriaService.registrar(
                "APLICAÇÃO DE DESCONTO",
                "Pedido",
                salvo.getId(),
                valoresAnteriores,
                valoresNovos,
                null
        );
        return salvo;
    }

    private StatusPedido interpretarStatus(String status) {
        try {
            return StatusPedido.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Status inválido.");
        }
    }

    private boolean transicaoPermitida(StatusPedido atual, StatusPedido proximo) {
        if (atual == null || proximo == null) {
            return false;
        }
        return switch (atual) {
            case RECEBIDO -> proximo == StatusPedido.EM_PREPARACAO;
            case EM_PREPARACAO -> proximo == StatusPedido.PRONTO;
            case PRONTO -> proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> proximo == StatusPedido.FINALIZADO;
            case FINALIZADO, CANCELADO -> false;
        };
    }

    private double descontoAplicavel(Double descontoInformado, double valorBruto) {
        if (descontoInformado == null || descontoInformado == 0.0) {
            return 0.0;
        }
        if (descontoInformado < 0) {
            throw new IllegalArgumentException("Desconto inválido. O máximo permitido é 20%.");
        }
        double desconto = arredondarMonetario(descontoInformado);
        double limite = arredondarMonetario(valorBruto * 0.20);
        if (desconto > limite) {
            throw new IllegalArgumentException("Desconto inválido. O máximo permitido é 20%.");
        }
        return desconto;
    }

    private String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SISTEMA";
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
