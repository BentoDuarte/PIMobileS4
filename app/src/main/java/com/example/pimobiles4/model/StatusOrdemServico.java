package com.example.pimobiles4.model;
public enum StatusOrdemServico {
    ABERTA("Aberta"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada");
    private final String descricao;
    StatusOrdemServico(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
    @Override public String toString() { return descricao; }
}
