package com.example.pimobiles4.email;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Repository
class OrdensFirestore {
    private final Firestore banco;
    private final DocumentReference usuario;
    private final DocumentReference estado;

    OrdensFirestore(Firestore banco, ConfiguracaoImportacao config) {
        this.banco = banco;
        usuario = banco.collection("usuarios").document(config.usuarioFirebase());
        var imap = config.imap();
        String origem = String.join("\0", imap.host(), Integer.toString(imap.porta()),
                imap.usuario(), imap.pasta());
        estado = usuario.collection("importacoesEmail").document(hash(origem));
    }

    long ultimoUid(long validade) throws ExecutionException, InterruptedException {
        return ultimoUid(estado.get().get(), validade);
    }

    boolean criar(EmailOrdem email, long validade, long uid)
            throws ExecutionException, InterruptedException {
        DocumentReference sequencia = usuario.collection("config").document("sequencia");
        DocumentReference registro = usuario.collection("registros").document();
        long agora = System.currentTimeMillis();
        String status = "ABERTA";

        return banco.runTransaction(transacao -> {
            if (uid <= ultimoUid(transacao.get(estado).get(), validade)) {
                return false;
            }
            Long ultimo = transacao.get(sequencia).get().getLong("ultimoId");
            long id = ultimo == null ? 1 : Math.incrementExact(ultimo);
            DocumentReference ordem = usuario.collection("ordens").document(Long.toString(id));

            transacao.create(ordem, Map.of(
                    "id", id,
                    "titulo", email.titulo(),
                    "cliente", Map.of("id", 0L, "nome", email.nomeCliente(),
                            "email", email.emailCliente(), "telefone", ""),
                    "descricao", email.descricao(),
                    "responsavel", "",
                    "prioridade", "MEDIA",
                    "status", status,
                    "criadaEm", agora,
                    "atualizadaEm", agora));
            transacao.create(registro, Map.of("ordemId", id, "titulo", email.titulo(),
                    "acao", "CRIACAO", "status", status, "data", FieldValue.serverTimestamp()));
            transacao.set(sequencia, Map.of("ultimoId", id));
            transacao.set(estado, Map.of("uidValidity", validade, "ultimoUid", uid));
            return true;
        }).get();
    }

    private static long ultimoUid(DocumentSnapshot documento, long validade) {
        if (!documento.exists()) return 0L;
        if (!Long.valueOf(validade).equals(documento.getLong("uidValidity"))) {
            throw new IllegalStateException("O UIDVALIDITY da pasta IMAP mudou.");
        }
        return documento.getLong("ultimoUid");
    }

    private static String hash(String texto) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException(erro);
        }
    }
}
