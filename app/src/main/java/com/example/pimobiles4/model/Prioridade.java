package com.example.pimobiles4.model;
public enum Prioridade {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta");
    private final String descricao;
    Prioridade(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
    @Override public String toString() { return descricao; }
}
