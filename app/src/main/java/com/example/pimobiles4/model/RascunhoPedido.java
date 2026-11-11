package com.example.pimobiles4.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dados sugeridos para um formulário ainda não salvo. */
public final class RascunhoPedido {
    public static final String EXTRA_TEXTO = "com.example.pimobiles4.TEXTO_PEDIDO";
    public static final int MAX_TEXTO = 20_000;

    private static final Pattern TITULO = rotulo("assunto|t[ií]tulo");
    private static final Pattern CLIENTE = rotulo("cliente|nome(?: do cliente)?|solicitante");
    private static final Pattern TELEFONE = rotulo("telefone|celular|contato");
    private static final Pattern EMAIL = Pattern.compile(
            "[a-z0-9._%+!-]+@[a-z0-9.-]+\\.[a-z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRICAO = rotulo("descri[cç][aã]o|problema|pedido");
    private static final Pattern NUMERO_TELEFONE = Pattern.compile("[+()\\d .-]+");

    public final String titulo;
    public final String cliente;
    public final String telefone;
    public final String email;
    public final String descricao;

    private RascunhoPedido(String titulo, String cliente, String telefone,
                           String email, String descricao) {
        this.titulo = titulo;
        this.cliente = cliente;
        this.telefone = telefone;
        this.email = email;
        this.descricao = descricao;
    }

    public static RascunhoPedido aPartirDoTexto(String entrada) {
        String texto = limitarTexto(entrada).replace("\r\n", "\n").replace('\r', '\n');
        String titulo = campo(TITULO, texto);
        String telefone = campo(TELEFONE, texto);
        String descricao = texto;
        Matcher inicioDescricao = DESCRICAO.matcher(texto);
        if (inicioDescricao.find()) {
            String trecho = texto.substring(inicioDescricao.start(1)).trim();
            if (!trecho.isEmpty()) descricao = trecho;
        }
        if (titulo.isEmpty() && !texto.isEmpty()) {
            String primeiraLinha = texto.split("\n", 2)[0];
            if (primeiraLinha.length() <= 120 && primeiraLinha.indexOf(':') < 0
                    && !EMAIL.matcher(primeiraLinha).find()) {
                titulo = primeiraLinha;
            }
        }
        Matcher email = EMAIL.matcher(texto);
        return new RascunhoPedido(titulo, campo(CLIENTE, texto),
                NUMERO_TELEFONE.matcher(telefone).matches() ? telefone : "",
                email.find() ? email.group() : "", descricao);
    }

    public static String limitarTexto(CharSequence texto) {
        if (texto == null) return "";
        int fim = Math.min(texto.length(), MAX_TEXTO);
        if (fim > 0 && Character.isHighSurrogate(texto.charAt(fim - 1))) fim--;
        return texto.subSequence(0, fim).toString().trim();
    }

    private static Pattern rotulo(String nomes) {
        return Pattern.compile("^[ \\t]*(?:" + nomes + ")[ \\t]*:[ \\t]*(.*)$",
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String campo(Pattern padrao, String texto) {
        Matcher campo = padrao.matcher(texto);
        return campo.find() ? campo.group(1).trim() : "";
    }
}
