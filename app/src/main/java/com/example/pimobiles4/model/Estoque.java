package com.example.pimobiles4.model;
/** Saldo local de referência. Movimentações serão validadas no backend. */
public class Estoque {
    private long produtoId;
    private int quantidadeDisponivel;
    public Estoque() {}
    public long getProdutoId() { return produtoId; }
    public void setProdutoId(long produtoId) { this.produtoId = produtoId; }
    public int getQuantidadeDisponivel() { return quantidadeDisponivel; }
    public void setQuantidadeDisponivel(int quantidadeDisponivel) { this.quantidadeDisponivel = quantidadeDisponivel; }
}
