package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public void registrar(String operacao, String entidade, String entidadeId,
                          String camposAlterados, String valorAnterior, String novoValor,
                          String justificativa) {
        Map<String, String> anteriores = new LinkedHashMap<>();
        Map<String, String> novos = new LinkedHashMap<>();
        String campo = camposAlterados != null && !camposAlterados.isBlank() ? camposAlterados : "valor";
        if (valorAnterior != null) {
            anteriores.put(campo, formatarValorAuditoria(valorAnterior));
        }
        if (novoValor != null) {
            novos.put(campo, formatarValorAuditoria(novoValor));
        }
        salvar(operacao, entidade, entidadeId, anteriores, novos, justificativa);
    }

    public void registrar(String operacao, String entidade, String entidadeId,
                          Map<String, String> valoresAnteriores, Map<String, String> valoresNovos,
                          String justificativa) {
        salvar(operacao, entidade, entidadeId, valoresAnteriores, valoresNovos, justificativa);
    }

    public List<Auditoria> listarTodas() {
        List<Auditoria> registros = auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
        for (Auditoria auditoria : registros) {
            auditoria.setValorAnterior(formatarValorAuditoria(auditoria.getValorAnterior()));
            auditoria.setNovoValor(formatarValorAuditoria(auditoria.getNovoValor()));
            auditoria.setValoresAnteriores(formatarMapa(auditoria.getValoresAnteriores()));
            auditoria.setValoresNovos(formatarMapa(auditoria.getValoresNovos()));
        }
        return registros;
    }

    private void salvar(String operacao, String entidade, String entidadeId,
                        Map<String, String> valoresAnteriores, Map<String, String> valoresNovos,
                        String justificativa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String usuario = "SISTEMA";
        String perfil = "N/A";

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            usuario = authentication.getName();
            perfil = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                    .findFirst()
                    .orElse("N/A");
        }

        Map<String, String> anteriores = formatarMapa(valoresAnteriores);
        Map<String, String> novos = formatarMapa(valoresNovos);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setPerfil(perfil);
        auditoria.setDataHora(LocalDateTime.now());
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setCamposAlterados(String.join(", ", novos.isEmpty() ? anteriores.keySet() : novos.keySet()));
        auditoria.setValoresAnteriores(anteriores);
        auditoria.setValoresNovos(novos);
        auditoria.setValorAnterior(resumir(anteriores));
        auditoria.setNovoValor(resumir(novos));
        auditoria.setJustificativa(justificativa);

        auditoriaRepository.save(auditoria);
    }

    private Map<String, String> formatarMapa(Map<String, String> valores) {
        Map<String, String> formatado = new LinkedHashMap<>();
        if (valores == null) {
            return formatado;
        }
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            formatado.put(entrada.getKey(), formatarValorAuditoria(entrada.getValue()));
        }
        return formatado;
    }

    private String resumir(Map<String, String> valores) {
        if (valores == null || valores.isEmpty()) {
            return null;
        }
        return valores.entrySet().stream()
                .map(entrada -> entrada.getKey() + "=" + entrada.getValue())
                .collect(Collectors.joining(", "));
    }

    private String formatarValorAuditoria(String valor) {
        if (valor == null || valor.isBlank() || !valor.trim().matches("-?\\d+\\.\\d+")) {
            return valor;
        }
        try {
            double numero = Double.parseDouble(valor.trim());
            return String.format(Locale.US, "%.2f", numero);
        } catch (NumberFormatException e) {
            return valor;
        }
    }
}
