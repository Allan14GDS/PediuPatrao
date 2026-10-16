package com.umc.pediupatrao.dto;

public class CancelamentoDTO {

    private String justificativa;

    public CancelamentoDTO() {
    }

    public CancelamentoDTO(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }
}
