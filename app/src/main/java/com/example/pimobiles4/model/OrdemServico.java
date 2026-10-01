package com.example.pimobiles4.model;

/** Modelo de transporte; não é uma entidade JPA. */
public class OrdemServico {
    private long id;
    private String titulo;
    private Cliente cliente;
    private String descricao;
    private String responsavel;
    private StatusOrdemServico status;
    private Prioridade prioridade;
    private long criadaEm;
    private long atualizadaEm;

    public OrdemServico() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public StatusOrdemServico getStatus() { return status; }
    public void setStatus(StatusOrdemServico status) { this.status = status; }
    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }
    public long getCriadaEm() { return criadaEm; }
    public void setCriadaEm(long criadaEm) { this.criadaEm = criadaEm; }
    public long getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(long atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
