package com.example.pimobiles4.model;

/** Modelo de transporte; não é uma entidade JPA. */
public class Produto {
    private long id;
    private String nome;
    private String codigo;
    private String unidadeMedida;
    private java.math.BigDecimal valorUnitario;

    public Produto() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public java.math.BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(java.math.BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
}
