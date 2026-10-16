package com.umc.pediupatrao.dto;

public class DescontoDTO {

    private Double percentual;

    public DescontoDTO() {
    }

    public DescontoDTO(Double percentual) {
        this.percentual = percentual;
    }

    public Double getPercentual() {
        return percentual;
    }

    public void setPercentual(Double percentual) {
        this.percentual = percentual;
    }
}
