package com.example.pimobiles4.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.pimobiles4.model.*;
import org.json.*;
import java.util.*;

/** Protótipo local. Substituir por API REST antes de uso multiusuário. */
public class LocalOrdemRepository implements OrdemRepository {
    private final SharedPreferences prefs;
    public LocalOrdemRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("ordens", Context.MODE_PRIVATE);
    }
    public List<OrdemServico> listar() {
        List<OrdemServico> resultado = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("dados", "[]"));
            for (int i=0; i<array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                OrdemServico o = new OrdemServico();
                o.setId(json.getLong("id")); o.setTitulo(json.getString("titulo"));
                o.setDescricao(json.getString("descricao")); o.setResponsavel(json.getString("responsavel"));
                o.setStatus(StatusOrdemServico.valueOf(json.getString("status")));
                o.setPrioridade(Prioridade.valueOf(json.getString("prioridade")));
                o.setCriadaEm(json.getLong("criadaEm")); o.setAtualizadaEm(json.getLong("atualizadaEm"));
                Cliente c = new Cliente(); c.setNome(json.getString("cliente"));
                c.setTelefone(json.optString("telefone")); c.setEmail(json.optString("email")); o.setCliente(c);
                resultado.add(o);
            }
        } catch (JSONException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível ler as ordens salvas. Os dados foram preservados.", e);
        }
        resultado.sort((a,b) -> Long.compare(b.getId(), a.getId()));
        return resultado;
    }
    public OrdemServico buscar(long id) {
        for (OrdemServico o : listar()) if(o.getId()==id) return o;
        return null;
    }
    public void salvar(OrdemServico ordem) {
        List<OrdemServico> lista = listar();
        if(ordem.getId()==0) {
            long maior = prefs.getLong("ultimoId", 0);
            for(OrdemServico o:lista) maior = Math.max(maior,o.getId());
            ordem.setId(maior+1); ordem.setCriadaEm(System.currentTimeMillis());
        }
        ordem.setAtualizadaEm(System.currentTimeMillis());
        lista.removeIf(o -> o.getId()==ordem.getId()); lista.add(ordem); gravar(lista, ordem.getId());
    }
    public void excluir(long id) {
        List<OrdemServico> lista = listar(); lista.removeIf(o -> o.getId()==id); gravar(lista,0);
    }
    private void gravar(List<OrdemServico> lista, long ultimo) {
        JSONArray array = new JSONArray();
        try {
            for(OrdemServico o:lista) {
                JSONObject x = new JSONObject();
                x.put("id",o.getId()); x.put("titulo",o.getTitulo()); x.put("descricao",o.getDescricao());
                x.put("responsavel",o.getResponsavel()); x.put("cliente",o.getCliente().getNome());
                x.put("telefone",o.getCliente().getTelefone()); x.put("email",o.getCliente().getEmail());
                x.put("status",o.getStatus().name()); x.put("prioridade",o.getPrioridade().name());
                x.put("criadaEm",o.getCriadaEm()); x.put("atualizadaEm",o.getAtualizadaEm()); array.put(x);
            }
        } catch(JSONException e) { throw new IllegalStateException("Falha ao preparar dados",e); }
        if(!prefs.edit().putString("dados",array.toString()).putLong("ultimoId",Math.max(ultimo,prefs.getLong("ultimoId",0))).commit())
            throw new IllegalStateException("Não foi possível salvar no aparelho");
    }
}
