package com.example.pimobiles4.model;

import java.util.Date;

public class RegistroOrdem {
    public enum Acao {
        CRIACAO("Criação"),
        EDICAO("Edição"),
        ALTERACAO_STATUS("Alteração de status"),
        EXCLUSAO("Exclusão");

        private final String rotulo;

        Acao(String rotulo) { this.rotulo = rotulo; }
        @Override public String toString() { return rotulo; }
    }

    private long ordemId;
    private String titulo;
    private Acao acao;
    private StatusOrdemServico status;
    private Date data;

    public RegistroOrdem() {}

    public long getOrdemId() { return ordemId; }
    public String getTitulo() { return titulo; }
    public Acao getAcao() { return acao; }
    public StatusOrdemServico getStatus() { return status; }
    public Date getData() { return data; }
}
