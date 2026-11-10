package com.example.pimobiles4.email;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

record EmailOrdem(String titulo, String nomeCliente, String emailCliente, String descricao) {
    static EmailOrdem ler(Message mensagem) throws MessagingException, IOException {
        Address[] remetentes = mensagem.getFrom();
        String nome = "";
        String email = "";
        if (remetentes != null && remetentes.length > 0
                && remetentes[0] instanceof InternetAddress remetente) {
            nome = remetente.getPersonal();
            email = limitar(remetente.getAddress(), "", 320);
        }
        return new EmailOrdem(
                limitar(mensagem.getSubject(), "E-mail sem assunto", 300),
                limitar(nome, email.isBlank() ? "Remetente desconhecido" : email, 300),
                email,
                limitar(corpo(mensagem), "E-mail sem corpo de texto.", 20000));
    }

    private static String corpo(Part parte) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition()) || parte.getFileName() != null) {
            return "";
        }
        if (parte.isMimeType("text/plain")) {
            return (String) parte.getContent();
        }
        if (parte.isMimeType("text/html")) {
            return Jsoup.parse((String) parte.getContent()).text();
        }
        if (!parte.isMimeType("multipart/*")) {
            return "";
        }

        Multipart multipart = (Multipart) parte.getContent();
        if (parte.isMimeType("multipart/alternative")) {
            // Procura texto puro primeiro. As demais alternativas são lidas em ordem de preferência.
            for (int i = 0; i < multipart.getCount(); i++) {
                Part alternativa = multipart.getBodyPart(i);
                if (alternativa.isMimeType("text/plain")) {
                    String texto = corpo(alternativa);
                    if (!texto.isBlank()) return texto;
                }
            }
            for (int i = multipart.getCount() - 1; i >= 0; i--) {
                Part alternativa = multipart.getBodyPart(i);
                if (alternativa.isMimeType("text/plain")) continue;
                String texto = corpo(alternativa);
                if (!texto.isBlank()) return texto;
            }
            return "";
        }

        List<String> textos = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            String texto = corpo(multipart.getBodyPart(i));
            if (!texto.isBlank()) textos.add(texto);
        }
        return String.join("\n\n", textos);
    }

    private static String limitar(String texto, String padrao, int maximo) {
        if (texto == null || texto.isBlank()) return padrao;
        texto = texto.strip();
        if (texto.codePointCount(0, texto.length()) <= maximo) return texto;
        return texto.substring(0, texto.offsetByCodePoints(0, maximo));
    }
}
