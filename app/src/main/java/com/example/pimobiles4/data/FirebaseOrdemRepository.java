package com.example.pimobiles4.data;

import com.example.pimobiles4.model.OrdemServico;
import com.example.pimobiles4.model.StatusOrdemServico;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FirebaseOrdemRepository {
    private final FirebaseFirestore banco = FirebaseFirestore.getInstance();
    private final DocumentReference usuario = banco.collection("usuarios")
            .document(FirebaseAuth.getInstance().getCurrentUser().getUid());
    private final CollectionReference ordens = usuario.collection("ordens");

    public Task<List<OrdemServico>> listar() {
        return ordens.orderBy("id", Query.Direction.DESCENDING).get()
                .onSuccessTask(resultado -> com.google.android.gms.tasks.Tasks.forResult(
                        resultado.toObjects(OrdemServico.class)));
    }

    public Task<OrdemServico> buscar(long id) {
        return documento(id).get().onSuccessTask(resultado ->
                com.google.android.gms.tasks.Tasks.forResult(resultado.toObject(OrdemServico.class)));
    }

    public Task<Void> salvar(OrdemServico ordem) {
        long agora = System.currentTimeMillis();
        if (ordem.getId() != 0) {
            Map<String, Object> campos = new HashMap<>();
            campos.put("titulo", ordem.getTitulo());
            campos.put("cliente", ordem.getCliente());
            campos.put("descricao", ordem.getDescricao());
            campos.put("responsavel", ordem.getResponsavel());
            campos.put("prioridade", ordem.getPrioridade().name());
            campos.put("atualizadaEm", agora);
            return documento(ordem.getId()).update(campos);
        }
        DocumentReference contador = usuario.collection("config").document("sequencia");
        return banco.runTransaction(transacao -> {
            Long ultimo = transacao.get(contador).getLong("ultimoId");
            long id = ultimo == null ? 1 : ultimo + 1;
            transacao.set(contador, Collections.singletonMap("ultimoId", id));
            Map<String, Object> campos = new HashMap<>();
            campos.put("id", id);
            campos.put("titulo", ordem.getTitulo());
            campos.put("cliente", ordem.getCliente());
            campos.put("descricao", ordem.getDescricao());
            campos.put("responsavel", ordem.getResponsavel());
            campos.put("prioridade", ordem.getPrioridade().name());
            campos.put("status", StatusOrdemServico.ABERTA.name());
            campos.put("criadaEm", agora);
            campos.put("atualizadaEm", agora);
            transacao.set(documento(id), campos);
            return null;
        });
    }

    public Task<Void> alterarStatus(long id, StatusOrdemServico status) {
        return documento(id).update("status", status.name(), "atualizadaEm", System.currentTimeMillis());
    }

    public Task<Void> excluir(long id) {
        return documento(id).delete();
    }

    private DocumentReference documento(long id) {
        return ordens.document(Long.toString(id));
    }
}
