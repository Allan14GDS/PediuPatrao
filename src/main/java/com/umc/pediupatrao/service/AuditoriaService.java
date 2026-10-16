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
import java.util.List;
import java.util.Locale;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public void registrar(String operacao, String entidade, String entidadeId,
                          String camposAlterados, String valorAnterior, String novoValor,
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

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setPerfil(perfil);
        auditoria.setDataHora(LocalDateTime.now());
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setCamposAlterados(camposAlterados);
        auditoria.setValorAnterior(formatarValorAuditoria(valorAnterior));
        auditoria.setNovoValor(formatarValorAuditoria(novoValor));
        auditoria.setJustificativa(justificativa);

        auditoriaRepository.save(auditoria);
    }

    public List<Auditoria> listarTodas() {
        List<Auditoria> registros = auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
        for (Auditoria auditoria : registros) {
            auditoria.setValorAnterior(formatarValorAuditoria(auditoria.getValorAnterior()));
            auditoria.setNovoValor(formatarValorAuditoria(auditoria.getNovoValor()));
        }
        return registros;
    }

    private String formatarValorAuditoria(String valor) {
        if (valor == null || valor.isBlank()) {
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
