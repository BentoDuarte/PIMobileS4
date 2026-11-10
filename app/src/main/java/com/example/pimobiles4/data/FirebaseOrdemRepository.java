package com.example.pimobiles4.data;

import com.example.pimobiles4.model.OrdemServico;
import com.example.pimobiles4.model.RegistroOrdem;
import com.example.pimobiles4.model.RegistroOrdem.Acao;
import com.example.pimobiles4.model.StatusOrdemServico;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirebaseOrdemRepository {
    private final FirebaseFirestore banco = FirebaseFirestore.getInstance();
    private final DocumentReference usuario = banco.collection("usuarios")
            .document(FirebaseAuth.getInstance().getCurrentUser().getUid());
    private final CollectionReference ordens = usuario.collection("ordens");
    private final CollectionReference registros = usuario.collection("registros");

    public Task<List<OrdemServico>> listar() {
        return ordens.orderBy("id", Query.Direction.DESCENDING).get()
                .onSuccessTask(resultado -> Tasks.forResult(
                        resultado.toObjects(OrdemServico.class)));
    }

    public Task<List<RegistroOrdem>> listarRegistros() {
        return registros.orderBy("data", Query.Direction.DESCENDING).get()
                .onSuccessTask(resultado -> Tasks.forResult(resultado.toObjects(RegistroOrdem.class)));
    }

    public Task<OrdemServico> buscar(long id) {
        return documento(id).get().onSuccessTask(resultado ->
                Tasks.forResult(resultado.toObject(OrdemServico.class)));
    }

    public Task<Void> salvar(OrdemServico ordem) {
        long agora = System.currentTimeMillis();
        Map<String, Object> campos = new HashMap<>();
        campos.put("titulo", ordem.getTitulo());
        campos.put("cliente", ordem.getCliente());
        campos.put("descricao", ordem.getDescricao());
        campos.put("responsavel", ordem.getResponsavel());
        campos.put("prioridade", ordem.getPrioridade().name());
        campos.put("atualizadaEm", agora);
        DocumentReference registro = registros.document();
        if (ordem.getId() != 0) {
            long id = ordem.getId();
            DocumentReference ref = documento(id);
            return banco.runTransaction(transacao -> {
                DocumentSnapshot atual = transacao.get(ref);
                exigirOrdem(atual);
                transacao.update(ref, campos);
                registrar(transacao, registro, id, ordem.getTitulo(), Acao.EDICAO,
                        atual.getString("status"));
                return null;
            });
        }
        DocumentReference contador = usuario.collection("config").document("sequencia");
        return banco.runTransaction(transacao -> {
            Long ultimo = transacao.get(contador).getLong("ultimoId");
            long id = ultimo == null ? 1 : ultimo + 1;
            transacao.set(contador, Collections.singletonMap("ultimoId", id));
            campos.put("id", id);
            campos.put("status", StatusOrdemServico.ABERTA.name());
            campos.put("criadaEm", agora);
            transacao.set(documento(id), campos);
            registrar(transacao, registro, id, ordem.getTitulo(), Acao.CRIACAO,
                    StatusOrdemServico.ABERTA.name());
            return null;
        });
    }

    public Task<Void> alterarStatus(long id, StatusOrdemServico status) {
        DocumentReference ref = documento(id);
        DocumentReference registro = registros.document();
        long agora = System.currentTimeMillis();
        return banco.runTransaction(transacao -> {
            DocumentSnapshot atual = transacao.get(ref);
            exigirOrdem(atual);
            if (status.name().equals(atual.getString("status"))) return null;
            transacao.update(ref, "status", status.name(), "atualizadaEm", agora);
            registrar(transacao, registro, id, atual.getString("titulo"), Acao.ALTERACAO_STATUS,
                    status.name());
            return null;
        });
    }

    public Task<Void> excluir(long id) {
        DocumentReference ref = documento(id);
        DocumentReference registro = registros.document();
        return banco.runTransaction(transacao -> {
            DocumentSnapshot atual = transacao.get(ref);
            if (!atual.exists()) return null;
            transacao.delete(ref);
            registrar(transacao, registro, id, atual.getString("titulo"), Acao.EXCLUSAO,
                    atual.getString("status"));
            return null;
        });
    }

    private void registrar(Transaction transacao, DocumentReference registro, long id,
                           String titulo, Acao acao, String status) {
        Map<String, Object> campos = new HashMap<>();
        campos.put("ordemId", id);
        campos.put("titulo", titulo);
        campos.put("acao", acao.name());
        campos.put("status", status);
        campos.put("data", FieldValue.serverTimestamp());
        transacao.set(registro, campos);
    }

    private void exigirOrdem(DocumentSnapshot ordem) throws FirebaseFirestoreException {
        if (!ordem.exists()) {
            throw new FirebaseFirestoreException("Ordem não encontrada.",
                    FirebaseFirestoreException.Code.NOT_FOUND);
        }
    }

    private DocumentReference documento(long id) {
        return ordens.document(Long.toString(id));
    }
}
