package com.example.pimobiles4.email;

import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

@Service
class ImportadorEmail {
    private static final int LIMITE_POR_EXECUCAO = 50;

    private final ConfiguracaoImportacao.Imap config;
    private final OrdensFirestore ordens;
    private final ReentrantLock execucao = new ReentrantLock();

    ImportadorEmail(ConfiguracaoImportacao config, OrdensFirestore ordens) {
        this.config = config.imap();
        this.ordens = ordens;
    }

    Resultado importar() throws MessagingException, IOException, ExecutionException, InterruptedException {
        if (!execucao.tryLock()) return new Resultado(0, true);
        try {
            return new Resultado(lerPasta(), false);
        } finally {
            execucao.unlock();
        }
    }

    private int lerPasta() throws MessagingException, IOException, ExecutionException, InterruptedException {
        Properties propriedades = new Properties();
        propriedades.setProperty("mail.imaps.ssl.checkserveridentity", "true");
        propriedades.setProperty("mail.imaps.connectiontimeout", "10000");
        propriedades.setProperty("mail.imaps.timeout", "15000");
        propriedades.setProperty("mail.imaps.writetimeout", "15000");
        if (config.proxyHost() != null && !config.proxyHost().isBlank()) {
            String prefixoProxy = "http".equals(config.proxyTipo())
                    ? "mail.imaps.proxy." : "mail.imaps.socks.";
            propriedades.setProperty(prefixoProxy + "host", config.proxyHost().strip());
            propriedades.setProperty(prefixoProxy + "port", Integer.toString(config.proxyPorta()));
        }
        Session sessao = Session.getInstance(propriedades);

        try (Store caixa = sessao.getStore("imaps")) {
            caixa.connect(config.host(), config.porta(), config.usuario(), config.senha());
            Folder pasta = caixa.getFolder(config.pasta());
            pasta.open(Folder.READ_ONLY);
            try {
                return importarPasta(pasta);
            } finally {
                if (pasta.isOpen()) pasta.close(false);
            }
        }
    }

    private int importarPasta(Folder pasta)
            throws MessagingException, IOException, ExecutionException, InterruptedException {
        UIDFolder uids = (UIDFolder) pasta;
        long validade = uids.getUIDValidity();
        if (validade <= 0) throw new MessagingException("UIDVALIDITY indisponível.");
        long ultimo = ordens.ultimoUid(validade);
        if (pasta.getMessageCount() == 0) return 0;
        Message[] mensagens = uids.getMessagesByUID(ultimo + 1, UIDFolder.LASTUID);
        FetchProfile perfil = new FetchProfile();
        perfil.add(UIDFolder.FetchProfileItem.UID);
        pasta.fetch(mensagens, perfil);

        // O IMAP pode devolver a última mensagem mesmo se ela já estiver no cursor.
        var pendentes = new TreeMap<Long, Message>();
        for (Message mensagem : mensagens) {
            long uid = uids.getUID(mensagem);
            if (uid <= 0) throw new MessagingException("UID da mensagem indisponível.");
            if (uid > ultimo) pendentes.put(uid, mensagem);
        }

        int criadas = 0;
        int processadas = 0;
        for (var entrada : pendentes.entrySet()) {
            if (processadas == LIMITE_POR_EXECUCAO) break;
            if (ordens.criar(EmailOrdem.ler(entrada.getValue()), validade, entrada.getKey())) {
                criadas++;
            }
            processadas++;
        }
        return criadas;
    }

    record Resultado(int ordensCriadas, boolean emAndamento) {}
}
