package com.example.pimobiles4.ui;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
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
    private OrdemServico ordem,pdfSnapshot; private FirebaseOrdemRepository repo;
    private final ActivityResultLauncher<String> exportar=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"),uri->{
        if(uri==null)return;
        if (pdfSnapshot != null) {
            salvarPdf(uri, pdfSnapshot);
            pdfSnapshot = null;
        } else {
            repo.buscar(getIntent().getLongExtra("ordemId", 0))
                    .addOnSuccessListener(this, registro -> salvarPdf(uri, registro))
                    .addOnFailureListener(this, this::aviso);
        }
    });

    private void salvarPdf(android.net.Uri uri, OrdemServico registro) {
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (registro == null) throw new IOException("Ordem não encontrada");
            if (out == null) throw new IOException("Não foi possível abrir o destino");
            OrdemPdf.escrever(registro, out);
        } catch (IOException e) {
            aviso(e);
            return;
        }
        Toast.makeText(this, "PDF salvo no local escolhido", Toast.LENGTH_LONG).show();
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(isFinishing())return;tela(R.layout.activity_detalhes);repo=new FirebaseOrdemRepository();
        findViewById(R.id.voltar).setOnClickListener(v->finish());
        findViewById(R.id.recarregarDetalhes).setOnClickListener(v -> carregar());
        findViewById(R.id.editar).setOnClickListener(v->{if(ordem!=null)startActivity(new Intent(this,FormularioOrdemActivity.class).putExtra("ordemId",ordem.getId()));});
        findViewById(R.id.status).setOnClickListener(v->alterarStatus());
        findViewById(R.id.excluir).setOnClickListener(v -> {
            if (ordem == null) return;
            long id = ordem.getId();
            new AlertDialog.Builder(this).setTitle("Excluir OS #" + id + "?")
                    .setMessage("A ordem será removida da sua conta.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Excluir", (dialogo, botao) ->
                            gravar(repo.excluir(id), this::finish))
                    .show();
        });
        findViewById(R.id.pdf).setOnClickListener(v->{if(ordem!=null){pdfSnapshot=ordem;exportar.launch("OS-"+ordem.getId()+".pdf");}});
    }
    @Override protected void onStart(){super.onStart();if(repo!=null)carregar();}
    private void carregar(){
        ordem = null;
        habilitarAcoes(false);
        ((TextView) findViewById(R.id.tituloDetalhe)).setText("");
        View recarregar = findViewById(R.id.recarregarDetalhes);
        TextView dados = findViewById(R.id.dados);
        recarregar.setVisibility(View.GONE);
        dados.setText("Carregando ordem…");
        repo.buscar(getIntent().getLongExtra("ordemId", 0))
                .addOnSuccessListener(this, registro -> {
                    if (registro == null) {
                        Toast.makeText(this, "Ordem não encontrada", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    ordem = registro;
                    mostrar();
                    habilitarAcoes(true);
                })
                .addOnFailureListener(this, erro -> {
                    dados.setText(mensagemDeErro(erro));
                    recarregar.setVisibility(View.VISIBLE);
                });
    }

    private void gravar(com.google.android.gms.tasks.Task<Void> tarefa, Runnable sucesso) {
        habilitarAcoes(false);
        tarefa.addOnCompleteListener(resultado -> {
            if (isDestroyed() || isFinishing()) return;
            if (resultado.isSuccessful()) sucesso.run();
            else {
                habilitarAcoes(ordem != null);
                aviso(resultado.getException());
            }
        });
    }

    private void habilitarAcoes(boolean habilitadas) {
        findViewById(R.id.status).setEnabled(habilitadas);
        findViewById(R.id.excluir).setEnabled(habilitadas);
        findViewById(R.id.editar).setEnabled(habilitadas);
        findViewById(R.id.pdf).setEnabled(habilitadas);
    }

    private void mostrar() {
        ((TextView)findViewById(R.id.tituloDetalhe)).setText("OS #"+ordem.getId()+" • "+ordem.getTitulo());
        ((TextView)findViewById(R.id.dados)).setText("Status: "+ordem.getStatus()+"\nPrioridade: "+ordem.getPrioridade()+"\n\nCliente: "+ordem.getCliente().getNome()+"\nTelefone: "+ordem.getCliente().getTelefone()+"\nE-mail: "+ordem.getCliente().getEmail()+"\nResponsável: "+ordem.getResponsavel()+"\n\nDescrição\n"+ordem.getDescricao()+"\n\nCriada em: "+data(ordem.getCriadaEm())+"\nAtualizada em: "+data(ordem.getAtualizadaEm()));
    }
    private String data(long t){return new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(t));}
    private void alterarStatus(){
        if(ordem==null)return;
        StatusOrdemServico[] valores=StatusOrdemServico.values();String[] nomes=new String[valores.length];for(int i=0;i<valores.length;i++)nomes[i]=valores[i].toString();
        final int[] escolhido={ordem.getStatus().ordinal()};
        new AlertDialog.Builder(this).setTitle("Alterar status").setSingleChoiceItems(nomes,escolhido[0],(d,w)->escolhido[0]=w).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{
            if (ordem != null) gravar(repo.alterarStatus(ordem.getId(), valores[escolhido[0]]), this::carregar);
        }).show();
    }
}
