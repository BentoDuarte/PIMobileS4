package com.example.pimobiles4.model;

/** Modelo de transporte; não é uma entidade JPA. */
public class Usuario {
    private long id;
    private String nome;
    private String email;
    private boolean ativo;

    public Usuario() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean getAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
