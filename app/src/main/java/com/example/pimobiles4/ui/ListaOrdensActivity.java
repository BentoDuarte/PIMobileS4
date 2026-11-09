package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.*;
import android.view.View;
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
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(isFinishing())return; tela(R.layout.activity_lista);
        repo = new FirebaseOrdemRepository();
        busca=findViewById(R.id.busca); filtro=findViewById(R.id.filtro);
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
        ((ListView)findViewById(R.id.lista)).setOnItemClickListener((p,v,pos,id)->startActivity(new Intent(this,DetalhesOrdemActivity.class).putExtra("ordemId",visiveis.get(pos).getId())));
        findViewById(R.id.nova).setOnClickListener(v->startActivity(new Intent(this,FormularioOrdemActivity.class)));
        findViewById(R.id.recarregarOrdens).setOnClickListener(v -> carregar());
        findViewById(R.id.sair).setOnClickListener(v->{com.google.firebase.auth.FirebaseAuth.getInstance().signOut();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));});
    }
    @Override protected void onStart() {
        super.onStart();
        if (repo != null) carregar();
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
            List<OrdemServico> novas=new ArrayList<>(); List<String> textos=new ArrayList<>();
            String consulta = normalizar(busca.getText().toString().trim());
            String numero = PREFIXO_OS.matcher(consulta).replaceFirst("");
            int statusSelecionado = filtro.getSelectedItemPosition();
            for(OrdemServico o:ordens) {
                boolean corresponde = Long.toString(o.getId()).contains(numero)
                        || normalizar(o.getTitulo()).contains(consulta)
                        || normalizar(o.getCliente().getNome()).contains(consulta);
                if (!corresponde || (statusSelecionado > 0
                        && o.getStatus() != StatusOrdemServico.values()[statusSelecionado - 1])) continue;
                novas.add(o);textos.add("OS #"+o.getId()+" • "+o.getTitulo()+"\n"+o.getCliente().getNome()+"\n"+o.getStatus()+" • Prioridade "+o.getPrioridade());
            }
            visiveis=novas;
            ((ListView)findViewById(R.id.lista)).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,textos));
            ((TextView)findViewById(R.id.contador)).setText(novas.isEmpty()?"Nenhuma ordem encontrada":novas.size() == 1 ? "1 ordem encontrada" : novas.size() + " ordens encontradas");
        }catch(IllegalStateException e){aviso(e);}
    }
    private String normalizar(String texto) {
        return ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
                .replaceAll("").toLowerCase(Locale.ROOT);
    }
}
