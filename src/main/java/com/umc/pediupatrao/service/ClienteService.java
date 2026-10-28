package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        Cliente salvo = clienteRepository.save(cliente);
        auditoriaService.registrar(
                "INCLUSÃO DE CLIENTE",
                "Cliente",
                salvo.getId(),
                "cliente",
                null,
                resumoCliente(salvo),
                null
        );
        return salvo;
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            cliente.setId(null);
            return novoCliente(cliente);
        }

        Cliente atual = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));

        Cliente salvo = clienteRepository.save(cliente);
        registrarAlteracaoCliente(atual, salvo);
        return salvo;
    }

    private void registrarAlteracaoCliente(Cliente anterior, Cliente atualizado) {
        Map<String, String> valoresAnteriores = new LinkedHashMap<>();
        Map<String, String> valoresNovos = new LinkedHashMap<>();

        compararCampo(valoresAnteriores, valoresNovos, "nome", anterior.getNome(), atualizado.getNome());
        compararCampo(valoresAnteriores, valoresNovos, "telefone", anterior.getTelefone(), atualizado.getTelefone());
        compararCampo(valoresAnteriores, valoresNovos, "cep", anterior.getCep(), atualizado.getCep());
        compararCampo(valoresAnteriores, valoresNovos, "logradouro", anterior.getLogradouro(), atualizado.getLogradouro());
        compararCampo(valoresAnteriores, valoresNovos, "numero", anterior.getNumero(), atualizado.getNumero());
        compararCampo(valoresAnteriores, valoresNovos, "complemento", anterior.getComplemento(), atualizado.getComplemento());
        compararCampo(valoresAnteriores, valoresNovos, "bairro", anterior.getBairro(), atualizado.getBairro());
        compararCampo(valoresAnteriores, valoresNovos, "cidade", anterior.getCidade(), atualizado.getCidade());
        compararCampo(valoresAnteriores, valoresNovos, "estado", anterior.getEstado(), atualizado.getEstado());
        compararCampo(valoresAnteriores, valoresNovos, "endereco", anterior.getEndereco(), atualizado.getEndereco());

        if (valoresAnteriores.isEmpty()) {
            return;
        }

        auditoriaService.registrar(
                "ALTERAÇÃO DE CLIENTE",
                "Cliente",
                atualizado.getId(),
                valoresAnteriores,
                valoresNovos,
                null
        );
    }

    private void compararCampo(Map<String, String> anteriores, Map<String, String> novos,
                               String nome, String valorAnterior, String valorNovo) {
        String anterior = valorAnterior != null ? valorAnterior : "";
        String atual = valorNovo != null ? valorNovo : "";
        if (!Objects.equals(anterior, atual)) {
            anteriores.put(nome, anterior.isBlank() ? "(vazio)" : anterior);
            novos.put(nome, atual.isBlank() ? "(vazio)" : atual);
        }
    }

    private String resumoCliente(Cliente cliente) {
        return "nome=" + nuloParaVazio(cliente.getNome())
                + ", telefone=" + nuloParaVazio(cliente.getTelefone())
                + ", endereco=" + formatarEndereco(cliente);
    }

    private String formatarEndereco(Cliente cliente) {
        List<String> partes = new ArrayList<>();
        adicionarSePreenchido(partes, cliente.getLogradouro());
        adicionarSePreenchido(partes, cliente.getNumero());
        adicionarSePreenchido(partes, cliente.getComplemento());
        adicionarSePreenchido(partes, cliente.getBairro());
        adicionarSePreenchido(partes, cliente.getCidade());
        adicionarSePreenchido(partes, cliente.getEstado());
        adicionarSePreenchido(partes, cliente.getCep());
        if (!partes.isEmpty()) {
            return String.join(", ", partes);
        }
        return nuloParaVazio(cliente.getEndereco());
    }

    private void adicionarSePreenchido(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }

    private String nuloParaVazio(String valor) {
        return valor != null ? valor : "";
    }

}
