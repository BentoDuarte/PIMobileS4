package com.example.pimobiles4.model;
public enum TipoMovimentacao {
    ENTRADA("Entrada"),
    SAIDA("Saída");
    private final String descricao;
    TipoMovimentacao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
    @Override public String toString() { return descricao; }
}
