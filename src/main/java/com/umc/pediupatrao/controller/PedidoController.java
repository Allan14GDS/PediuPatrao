package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoDTO;
import com.umc.pediupatrao.dto.DescontoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/pedidos")
    public String listarPedidosView(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidos());
        model.addAttribute("content", "pedidos :: content");
        log.info("Carregando fragmento: pedidos :: content");
        return "layout";
    }

    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(Model model) {
        Pedido pedido = new Pedido();
        List<ItemPedido> itens = new ArrayList<>();
        itens.add(new ItemPedido());
        pedido.setItens(itens);
        model.addAttribute("pedido", pedido);
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("content", "pedidos/form :: content");
        log.info("Carregando fragmento: pedidos/form :: content");
        return "layout";
    }

    @PostMapping("/pedidos/salvar")
    public String salvarPedido(@ModelAttribute Pedido pedido,
            RedirectAttributes redirectAttributes) {
        try {
            pedidoService.criarPedido(pedido);
            redirectAttributes.addFlashAttribute("sucesso", "Pedido criado com sucesso!");
            return "redirect:/pedidos";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/novo";
        }
    }

    @PostMapping("/api/pedidos")
    @ResponseBody
    public Pedido criarPedido(@RequestBody Pedido pedido) {
        return pedidoService.criarPedido(pedido);
    }

    @GetMapping("/api/pedidos")
    @ResponseBody
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/api/pedidos/{id}/status")
    @ResponseBody
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, status);
    }

    @PostMapping("/api/pedidos/{id}/cancelar")
    @ResponseBody
    public Pedido cancelarPedido(@PathVariable String id, @RequestBody CancelamentoDTO dto) {
        return pedidoService.cancelarPedido(id, dto.getJustificativa());
    }

    @PostMapping("/api/pedidos/{id}/desconto")
    @ResponseBody
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoDTO dto) {
        return pedidoService.aplicarDesconto(id, dto.getPercentual());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleRegraNegocio(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
