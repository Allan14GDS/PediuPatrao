package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoDTO;
import com.umc.pediupatrao.dto.DescontoDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public Pedido criarPedido(@RequestBody Pedido pedido) {
        return pedidoService.criarPedido(pedido);
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, status);
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelarPedido(@PathVariable String id, @RequestBody CancelamentoDTO dto) {
        return pedidoService.cancelarPedido(id, dto.getJustificativa());
    }

    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoDTO dto) {
        return pedidoService.aplicarDesconto(id, dto.getPercentual());
    }
}
