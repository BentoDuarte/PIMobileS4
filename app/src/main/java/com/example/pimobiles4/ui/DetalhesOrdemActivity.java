package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.pimobiles4.R;
import com.example.pimobiles4.data.*;
import com.example.pimobiles4.model.*;
import com.example.pimobiles4.util.OrdemPdf;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
public class DetalhesOrdemActivity extends BaseActivity {
    private OrdemServico ordem,pdfSnapshot; private LocalOrdemRepository repo;
    private final ActivityResultLauncher<String> exportar=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),uri->{
        if(uri==null)return;
        try{
            if(pdfSnapshot==null)pdfSnapshot=repo.buscar(getIntent().getLongExtra("ordemId",0));
            if(pdfSnapshot==null)throw new IOException("Ordem não encontrada");
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                if(out==null)throw new IOException("Não foi possível abrir o destino");
                OrdemPdf.escrever(pdfSnapshot,out);
            }
            Toast.makeText(this,"PDF salvo no local escolhido",Toast.LENGTH_LONG).show();
        }catch(IOException|IllegalStateException e){aviso(e);}
    });
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(isFinishing())return;tela(R.layout.activity_detalhes);repo=new LocalOrdemRepository(this);
        findViewById(R.id.voltar).setOnClickListener(v->finish());
        findViewById(R.id.editar).setOnClickListener(v->{if(ordem!=null)startActivity(new Intent(this,FormularioOrdemActivity.class).putExtra("ordemId",ordem.getId()));});
        findViewById(R.id.status).setOnClickListener(v->alterarStatus());
        findViewById(R.id.excluir).setOnClickListener(v->{if(ordem!=null)new AlertDialog.Builder(this).setTitle("Excluir OS #"+ordem.getId()+"?").setMessage("A ordem será removida deste aparelho. Esta ação não pode ser desfeita.").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w)->{try{repo.excluir(ordem.getId());finish();}catch(IllegalStateException e){aviso(e);}}).show();});
        findViewById(R.id.pdf).setOnClickListener(v->{if(ordem!=null){pdfSnapshot=ordem;exportar.launch("OS-"+ordem.getId()+".pdf");}});
    }
    @Override protected void onResume(){super.onResume();if(repo!=null)carregar();}
    private void carregar(){
        try{ordem=repo.buscar(getIntent().getLongExtra("ordemId",0));}catch(IllegalStateException e){aviso(e);finish();return;}
        if(ordem==null){Toast.makeText(this,"Ordem não encontrada",Toast.LENGTH_SHORT).show();finish();return;}
        ((TextView)findViewById(R.id.tituloDetalhe)).setText("OS #"+ordem.getId()+" • "+ordem.getTitulo());
        ((TextView)findViewById(R.id.dados)).setText("Status: "+ordem.getStatus()+"\nPrioridade: "+ordem.getPrioridade()+"\n\nCliente: "+ordem.getCliente().getNome()+"\nTelefone: "+ordem.getCliente().getTelefone()+"\nE-mail: "+ordem.getCliente().getEmail()+"\nResponsável: "+ordem.getResponsavel()+"\n\nDescrição\n"+ordem.getDescricao()+"\n\nCriada em: "+data(ordem.getCriadaEm())+"\nAtualizada em: "+data(ordem.getAtualizadaEm()));
    }
    private String data(long t){return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(t));}
    private void alterarStatus(){
        if(ordem==null)return;
        StatusOrdemServico[] valores=StatusOrdemServico.values();String[] nomes=new String[valores.length];for(int i=0;i<valores.length;i++)nomes[i]=valores[i].toString();
        final int[] escolhido={ordem.getStatus().ordinal()};
        new AlertDialog.Builder(this).setTitle("Alterar status").setSingleChoiceItems(nomes,escolhido[0],(d,w)->escolhido[0]=w).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
            ordem.setStatus(valores[escolhido[0]]);try{repo.salvar(ordem);carregar();}catch(IllegalStateException e){aviso(e);}
        }).show();
    }
}
