package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.widget.*;
import android.util.Patterns;
import com.example.pimobiles4.R;
import com.example.pimobiles4.data.*;
import com.example.pimobiles4.model.*;
public class FormularioOrdemActivity extends BaseActivity {
    private OrdemServico ordem; private LocalOrdemRepository repo;
    private EditText titulo,cliente,telefone,email,descricao,responsavel; private Spinner prioridade;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(isFinishing())return;tela(R.layout.activity_formulario);
        repo=new LocalOrdemRepository(this);
        titulo=findViewById(R.id.titulo);cliente=findViewById(R.id.cliente);telefone=findViewById(R.id.telefone);
        email=findViewById(R.id.emailCliente);descricao=findViewById(R.id.descricao);responsavel=findViewById(R.id.responsavel);
        prioridade=findViewById(R.id.prioridade);prioridade.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Prioridade.values()));
        long id=getIntent().getLongExtra("ordemId",0);
        try {ordem=id==0?new OrdemServico():repo.buscar(id);}catch(IllegalStateException e){aviso(e);finish();return;}
        if(ordem==null){Toast.makeText(this,"Ordem não encontrada",Toast.LENGTH_SHORT).show();finish();return;}
        if(id!=0){
            ((TextView)findViewById(R.id.cabecalho)).setText("Editar OS #"+id);
            titulo.setText(ordem.getTitulo());cliente.setText(ordem.getCliente().getNome());telefone.setText(ordem.getCliente().getTelefone());email.setText(ordem.getCliente().getEmail());
            descricao.setText(ordem.getDescricao());responsavel.setText(ordem.getResponsavel());prioridade.setSelection(ordem.getPrioridade().ordinal());
        }else{ordem.setStatus(StatusOrdemServico.ABERTA);prioridade.setSelection(1);}
        findViewById(R.id.cancelar).setOnClickListener(v->finish());findViewById(R.id.salvar).setOnClickListener(v->salvar());
    }
    private String valor(EditText v){return v.getText().toString().trim();}
    private boolean obrigatorio(EditText v){if(valor(v).isEmpty()){v.setError("Campo obrigatório");v.requestFocus();return false;}return true;}
    private void salvar(){
        if(!obrigatorio(titulo)||!obrigatorio(cliente)||!obrigatorio(descricao))return;
        if(!valor(email).isEmpty()&&!Patterns.EMAIL_ADDRESS.matcher(valor(email)).matches()){email.setError("E-mail inválido");return;}
        Cliente c=new Cliente();c.setNome(valor(cliente));c.setTelefone(valor(telefone));c.setEmail(valor(email));
        ordem.setCliente(c);ordem.setTitulo(valor(titulo));ordem.setDescricao(valor(descricao));ordem.setResponsavel(valor(responsavel));ordem.setPrioridade((Prioridade)prioridade.getSelectedItem());
        try{repo.salvar(ordem);Toast.makeText(this,"Ordem salva",Toast.LENGTH_SHORT).show();finish();}catch(IllegalStateException e){aviso(e);}
    }
}
