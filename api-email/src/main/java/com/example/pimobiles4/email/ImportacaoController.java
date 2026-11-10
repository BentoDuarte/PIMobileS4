package com.example.pimobiles4.email;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
class ImportacaoController {
    private static final Logger LOG = LoggerFactory.getLogger(ImportacaoController.class);

    private final ImportadorEmail importador;
    private final byte[] autorizacao;
    private final ConfiguracaoImportacao.Imap imap;

    ImportacaoController(ImportadorEmail importador, ConfiguracaoImportacao config) {
        this.importador = importador;
        imap = config.imap();
        autorizacao = ("Bearer " + config.token()).getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/status")
    Map<String, String> status() {
        return Map.of("status", "ok");
    }

    @PostMapping("/api/importacoes/email")
    ResponseEntity<ImportadorEmail.Resultado> importar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, defaultValue = "") String cabecalho) {
        if (!MessageDigest.isEqual(autorizacao, cabecalho.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    .build();
        }
        ImportadorEmail.Resultado resultado = executar();
        if (resultado == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        if (resultado.emAndamento()) return ResponseEntity.status(HttpStatus.CONFLICT).body(resultado);
        return ResponseEntity.ok(resultado);
    }

    @Scheduled(cron = "${importacao.cron}")
    void importarAgendado() {
        executar();
    }

    private ImportadorEmail.Resultado executar() {
        try {
            var resultado = importador.importar();
            if (resultado.ordensCriadas() > 0) {
                LOG.info("Ordens criadas a partir de e-mail: {}", resultado.ordensCriadas());
            }
            return resultado;
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            LOG.warn("Importação de e-mail interrompida.");
        } catch (MessagingException erro) {
            String proxy = imap.proxyHost();
            String tipo = "http".equals(imap.proxyTipo()) ? "HTTP CONNECT" : "SOCKS5";
            String rota = proxy == null || proxy.isBlank()
                    ? "direta" : tipo + " " + proxy.strip() + ":" + imap.proxyPorta();
            Throwable causa = erro;
            while (causa.getCause() != null && causa.getCause() != causa) {
                causa = causa.getCause();
            }
            String detalhe = causa instanceof SocketException || causa instanceof SocketTimeoutException
                    ? causa.getClass().getSimpleName() + ": " + causa.getMessage()
                    : causa.getClass().getSimpleName();
            LOG.warn("Falha no IMAP {}:{} via {} ({})", imap.host(), imap.porta(), rota, detalhe);
        } catch (Exception erro) {
            LOG.warn("Falha na importação de e-mail ({})", erro.getClass().getSimpleName());
        }
        return null;
    }
}
