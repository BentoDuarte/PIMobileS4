package com.example.pimobiles4.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SimilaridadeOrdens {
    private static final int LIMITE = 3;
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern WIFI = Pattern.compile("\\bwi[ -]?fi\\b");
    private static final Pattern SEPARADOR = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> PALAVRAS_COMUNS = new HashSet<>(java.util.Arrays.asList(
            "com", "dos", "das", "para", "uma", "por", "que", "esta", "estao", "hoje", "favor",
            "preciso", "solicito", "problema", "pedido", "chamado"));

    private SimilaridadeOrdens() {}

    public static boolean temTermosSuficientes(String consulta) {
        return termos(consulta).size() >= 2;
    }

    public static List<OrdemServico> buscar(String titulo, String descricao, List<OrdemServico> ordens) {
        Set<String> tituloConsulta = termos(titulo);
        Set<String> termosConsulta = termos(descricao);
        termosConsulta.addAll(tituloConsulta);
        List<Correspondencia> correspondencias = new ArrayList<>();
        if (termosConsulta.size() < 2) return new ArrayList<>();

        for (OrdemServico ordem : ordens) {
            Set<String> noTitulo = termos(ordem.getTitulo());
            Set<String> naOrdem = termos(ordem.getDescricao());
            naOrdem.addAll(noTitulo);
            naOrdem.retainAll(termosConsulta);
            Set<String> entreTitulos = new HashSet<>(noTitulo);
            entreTitulos.retainAll(tituloConsulta);
            boolean tituloParecido = entreTitulos.size() >= 2
                    && entreTitulos.size() * 2 >= tituloConsulta.size();
            boolean textoParecido = naOrdem.size() >= 2 && naOrdem.size() * 2 >= termosConsulta.size();
            if (!tituloParecido && !textoParecido) continue;
            noTitulo.retainAll(termosConsulta);
            int pontos = 2 * naOrdem.size() + noTitulo.size() + entreTitulos.size();
            correspondencias.add(new Correspondencia(ordem, pontos));
        }
        correspondencias.sort(Comparator.comparingInt((Correspondencia item) -> item.pontos)
                .thenComparingLong(item -> item.ordem.getId()).reversed());
        List<OrdemServico> sugestoes = new ArrayList<>();
        for (int i = 0; i < Math.min(LIMITE, correspondencias.size()); i++) {
            sugestoes.add(correspondencias.get(i).ordem);
        }
        return sugestoes;
    }

    private static Set<String> termos(String texto) {
        Set<String> termos = new HashSet<>();
        if (texto == null) return termos;
        String normalizado = ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
        normalizado = WIFI.matcher(normalizado).replaceAll("wifi");
        for (String termo : SEPARADOR.split(normalizado)) {
            if (termo.length() >= 3 && !PALAVRAS_COMUNS.contains(termo)) {
                termos.add(termoCanonico(termo));
            }
        }
        return termos;
    }

    private static String termoCanonico(String termo) {
        switch (termo) {
            case "wifi":
            case "internet":
            case "conexao":
            case "rede":
                return "rede";
            case "cai":
            case "caindo":
            case "quedas":
                return "queda";
            default:
                return termo;
        }
    }

    private static final class Correspondencia {
        private final OrdemServico ordem;
        private final int pontos;

        private Correspondencia(OrdemServico ordem, int pontos) {
            this.ordem = ordem;
            this.pontos = pontos;
        }
    }
}
