package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.text.*;
import com.example.pimobiles4.*;
import com.example.pimobiles4.model.*;
import com.example.pimobiles4.data.*;
import java.util.*;
public class ListaOrdensActivity extends BaseActivity {
    private EditText busca; private Spinner filtro;
    private List<OrdemServico> visiveis = new ArrayList<>();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(isFinishing())return; tela(R.layout.activity_lista);
        busca=findViewById(R.id.busca); filtro=findViewById(R.id.filtro);
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
        findViewById(R.id.sair).setOnClickListener(v->{getSharedPreferences("sessao",MODE_PRIVATE).edit().clear().apply();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));});
    }
    @Override protected void onResume(){super.onResume();if(busca!=null)atualizar();}
    private void atualizar(){
        try {
            List<OrdemServico> novas=new ArrayList<>(); List<String> textos=new ArrayList<>();
            String q=busca.getText().toString().trim().toLowerCase(Locale.ROOT);
            for(OrdemServico o:new LocalOrdemRepository(this).listar()) {
                String alvo=(o.getId()+" "+o.getTitulo()+" "+o.getCliente().getNome()).toLowerCase(Locale.ROOT);
                if(!alvo.contains(q) || (filtro.getSelectedItemPosition()>0 && o.getStatus()!=StatusOrdemServico.values()[filtro.getSelectedItemPosition()-1]))continue;
                novas.add(o);textos.add("OS #"+o.getId()+" • "+o.getTitulo()+"\n"+o.getCliente().getNome()+"\n"+o.getStatus()+" • Prioridade "+o.getPrioridade());
            }
            visiveis=novas;
            ((ListView)findViewById(R.id.lista)).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,textos));
            ((TextView)findViewById(R.id.contador)).setText(novas.isEmpty()?"Nenhuma ordem encontrada":novas.size()+" ordem(ns) encontrada(s)");
        }catch(IllegalStateException e){aviso(e);}
    }
}
