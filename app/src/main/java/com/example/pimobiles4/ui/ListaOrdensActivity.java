package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.ViewCompat;
import com.example.pimobiles4.*;
import com.example.pimobiles4.model.*;
import com.example.pimobiles4.data.*;
import java.util.*;
import java.text.Normalizer;
import java.util.regex.Pattern;
public class ListaOrdensActivity extends BaseActivity {
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern PREFIXO_OS = Pattern.compile("^(?:os\\s*#?\\s*|#\\s*)(?=\\d)");
    private EditText busca; private Spinner filtro;
    private FirebaseOrdemRepository repo;
    private boolean listaDisponivel;
    private List<OrdemServico> ordens = Collections.emptyList();
    private List<OrdemServico> visiveis = new ArrayList<>();
    private final Set<Long> excluindo = new HashSet<>();
    private OrdensAdapter adapter;
    private long expandida;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(isFinishing())return; tela(R.layout.activity_lista);
        repo = new FirebaseOrdemRepository();
        if (state != null) expandida = state.getLong("ordemExpandida");
        busca=findViewById(R.id.busca); filtro=findViewById(R.id.filtro);
        ListView lista = findViewById(R.id.lista);
        lista.setItemsCanFocus(true);
        adapter = new OrdensAdapter();
        lista.setAdapter(adapter);
        busca.setOnEditorActionListener((campo, acao, evento) -> {
            if (acao != android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) return false;
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), busca)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime());
            busca.clearFocus();
            return true;
        });
        filtro.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Todos os status","Aberta","Em andamento","Concluída","Cancelada"}));
        filtro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){atualizar();}
            public void onNothingSelected(AdapterView<?> p){}
        });
        busca.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){atualizar();}
            public void afterTextChanged(Editable e){}
        });
        findViewById(R.id.nova).setOnClickListener(v->startActivity(new Intent(this,FormularioOrdemActivity.class)));
        findViewById(R.id.recarregarOrdens).setOnClickListener(v -> carregar());
        findViewById(R.id.sair).setOnClickListener(v->{com.google.firebase.auth.FirebaseAuth.getInstance().signOut();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));});
    }
    @Override protected void onStart() {
        super.onStart();
        if (repo != null) carregar();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("ordemExpandida", expandida);
    }

    private void carregar() {
        View tentarNovamente = findViewById(R.id.recarregarOrdens);
        TextView contador = findViewById(R.id.contador);
        listaDisponivel = false;
        tentarNovamente.setVisibility(View.GONE);
        contador.setText("Carregando ordens…");
        repo.listar()
                .addOnSuccessListener(this, resultado -> {
                    ordens = resultado;
                    listaDisponivel = true;
                    atualizar();
                })
                .addOnFailureListener(this, erro -> {
                    contador.setText(mensagemDeErro(erro));
                    tentarNovamente.setVisibility(View.VISIBLE);
                });
    }

    private void atualizar(){
        if (!listaDisponivel) return;
        try {
            List<OrdemServico> novas = new ArrayList<>();
            String consulta = normalizar(busca.getText().toString().trim());
            String numero = PREFIXO_OS.matcher(consulta).replaceFirst("");
            int statusSelecionado = filtro.getSelectedItemPosition();
            for(OrdemServico o:ordens) {
                boolean corresponde = Long.toString(o.getId()).contains(numero)
                        || normalizar(o.getTitulo()).contains(consulta)
                        || normalizar(o.getCliente().getNome()).contains(consulta);
                if (!corresponde || (statusSelecionado > 0
                        && o.getStatus() != StatusOrdemServico.values()[statusSelecionado - 1])) continue;
                novas.add(o);
            }
            visiveis=novas;
            adapter.notifyDataSetChanged();
            ((TextView)findViewById(R.id.contador)).setText(novas.isEmpty()?"Nenhuma ordem encontrada":novas.size() == 1 ? "1 ordem encontrada" : novas.size() + " ordens encontradas");
        }catch(IllegalStateException e){aviso(e);}
    }

    private void excluir(long id) {
        if (!excluindo.add(id)) return;
        adapter.notifyDataSetChanged();
        repo.excluir(id).addOnCompleteListener(resultado -> {
            if (isFinishing() || isDestroyed()) return;
            excluindo.remove(id);
            if (resultado.isSuccessful()) {
                if (expandida == id) expandida = 0;
                ordens.removeIf(ordem -> ordem.getId() == id);
                visiveis.removeIf(ordem -> ordem.getId() == id);
                atualizar();
                Toast.makeText(this, "Ordem excluída", Toast.LENGTH_SHORT).show();
            } else {
                aviso(resultado.getException());
            }
            adapter.notifyDataSetChanged();
        });
    }

    private class OrdensAdapter extends BaseAdapter {
        @Override public int getCount() { return visiveis.size(); }

        @Override public OrdemServico getItem(int position) { return visiveis.get(position); }

        @Override public long getItemId(int position) { return getItem(position).getId(); }

        @Override public boolean hasStableIds() { return true; }

        @Override public View getView(int position, View item, ViewGroup parent) {
            if (item == null) {
                item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ordem, parent, false);
            }

            OrdemServico ordem = getItem(position);
            long id = ordem.getId();
            boolean aberta = expandida == id;
            boolean excluindoOrdem = excluindo.contains(id);
            Cliente cliente = ordem.getCliente();

            TextView resumo = item.findViewById(R.id.resumoOrdem);
            resumo.setText((aberta ? "▴ " : "▾ ") + "OS #" + id + " • " + ordem.getTitulo()
                    + "\n" + cliente.getNome() + "\n" + ordem.getStatus()
                    + " • Prioridade " + ordem.getPrioridade());
            ViewCompat.setStateDescription(resumo, aberta ? "Expandida" : "Recolhida");
            resumo.setOnClickListener(v -> {
                expandida = expandida == id ? 0 : id;
                notifyDataSetChanged();
            });

            View conteudo = item.findViewById(R.id.conteudoOrdem);
            conteudo.setVisibility(aberta ? View.VISIBLE : View.GONE);
            if (!aberta) return item;

            TextView infos = item.findViewById(R.id.infosOrdem);
            infos.setText("Telefone: " + valorOpcional(cliente.getTelefone())
                    + "\nE-mail: " + valorOpcional(cliente.getEmail())
                    + "\nResponsável: " + valorOpcional(ordem.getResponsavel())
                    + "\n\nDescrição\n" + ordem.getDescricao());

            View editar = item.findViewById(R.id.editarNaLista);
            editar.setEnabled(!excluindoOrdem);
            editar.setOnClickListener(v -> startActivity(new Intent(ListaOrdensActivity.this,
                    FormularioOrdemActivity.class).putExtra("ordemId", id)));
            TextView excluir = item.findViewById(R.id.excluirNaLista);
            excluir.setEnabled(!excluindoOrdem);
            excluir.setText(excluindoOrdem ? "Excluindo…" : "Excluir");
            excluir.setOnClickListener(v -> confirmarExclusao(id, () -> excluir(id)));
            View detalhes = item.findViewById(R.id.detalhesNaLista);
            detalhes.setEnabled(!excluindoOrdem);
            detalhes.setOnClickListener(v -> startActivity(new Intent(ListaOrdensActivity.this,
                    DetalhesOrdemActivity.class).putExtra("ordemId", id)));
            return item;
        }
    }

    private String valorOpcional(String valor) {
        return android.text.TextUtils.isEmpty(valor) ? "Não informado" : valor;
    }

    private String normalizar(String texto) {
        return ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
    }
}
