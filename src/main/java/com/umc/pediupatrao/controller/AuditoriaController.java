package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping("/auditoria")
    public String listar(Model model) {
        model.addAttribute("auditorias", auditoriaService.listarTodas());
        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }

    @GetMapping("/api/auditoria")
    @ResponseBody
    public List<Auditoria> listarJson() {
        return auditoriaService.listarTodas();
    }
}
