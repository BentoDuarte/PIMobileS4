package com.example.pimobiles4.email;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailOrdemTest {
    @Test
    void usaTextoPuroDoEmailMultipartEIgnoraAnexo() throws Exception {
        MimeBodyPart texto = new MimeBodyPart();
        texto.setText("A impressora parou.", "UTF-8");
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("<p>A impressora parou.</p>", "text/html; charset=UTF-8");
        MimeMultipart alternativas = new MimeMultipart("alternative");
        alternativas.addBodyPart(texto);
        alternativas.addBodyPart(html);

        MimeBodyPart corpo = new MimeBodyPart();
        corpo.setContent(alternativas);
        MimeBodyPart anexo = new MimeBodyPart();
        anexo.setText("Conteúdo do anexo.", "UTF-8");
        anexo.setFileName("diagnostico.txt");
        MimeMultipart partes = new MimeMultipart();
        partes.addBodyPart(corpo);
        partes.addBodyPart(anexo);

        MimeMessage mensagem = novaMensagem();
        mensagem.setFrom(new InternetAddress("ana@example.com", "Ana Silva", "UTF-8"));
        mensagem.setSubject("Manutenção", "UTF-8");
        mensagem.setContent(partes);
        mensagem.saveChanges();

        assertEquals(new EmailOrdem("Manutenção", "Ana Silva", "ana@example.com", "A impressora parou."),
                EmailOrdem.ler(mensagem));
    }

    @Test
    void converteHtmlEmTextoEUsaEmailQuandoFaltaNome() throws Exception {
        MimeMessage mensagem = novaMensagem();
        mensagem.setFrom(new InternetAddress("ana@example.com"));
        mensagem.setContent("<p>Falha na <b>rede</b> &amp; no Wi-Fi.</p>", "text/html; charset=UTF-8");
        mensagem.saveChanges();

        assertEquals(new EmailOrdem("E-mail sem assunto", "ana@example.com", "ana@example.com",
                "Falha na rede & no Wi-Fi."), EmailOrdem.ler(mensagem));
    }

    @Test
    void usaDescricaoPadraoQuandoFaltaTexto() throws Exception {
        MimeMessage mensagem = novaMensagem();
        mensagem.setText(" ", "UTF-8");
        mensagem.saveChanges();

        assertEquals("E-mail sem corpo de texto.", EmailOrdem.ler(mensagem).descricao());
    }

    private static MimeMessage novaMensagem() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
