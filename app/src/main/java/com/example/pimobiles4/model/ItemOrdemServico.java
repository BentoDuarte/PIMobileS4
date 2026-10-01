package com.example.pimobiles4.model;

/** Modelo de transporte; não é uma entidade JPA. */
public class ItemOrdemServico {
    private long id;
    private long ordemServicoId;
    private long produtoId;
    private String descricao;
    private int quantidade;
    private java.math.BigDecimal valorUnitario;

    public ItemOrdemServico() {}
    public long getOrdemServicoId() { return ordemServicoId; }
    public void setOrdemServicoId(long ordemServicoId) { this.ordemServicoId = ordemServicoId; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProdutoId() { return produtoId; }
    public void setProdutoId(long produtoId) { this.produtoId = produtoId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public java.math.BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(java.math.BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
}
