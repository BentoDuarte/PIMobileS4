package com.example.pimobiles4.model;

/** Modelo de transporte; não é uma entidade JPA. */
public class MovimentacaoEstoque {
    private long id;
    private long produtoId;
    private Long ordemServicoId;
    private TipoMovimentacao tipo;
    private int quantidade;
    private long dataEpochMillis;

    public MovimentacaoEstoque() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getProdutoId() { return produtoId; }
    public void setProdutoId(long produtoId) { this.produtoId = produtoId; }
    public Long getOrdemServicoId() { return ordemServicoId; }
    public void setOrdemServicoId(Long ordemServicoId) { this.ordemServicoId = ordemServicoId; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public long getDataEpochMillis() { return dataEpochMillis; }
    public void setDataEpochMillis(long dataEpochMillis) { this.dataEpochMillis = dataEpochMillis; }
}
