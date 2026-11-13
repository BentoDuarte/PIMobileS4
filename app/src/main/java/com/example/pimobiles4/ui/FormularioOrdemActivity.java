package com.example.pimobiles4.ui;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.util.Patterns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.pimobiles4.R;
import com.example.pimobiles4.data.*;
import com.example.pimobiles4.model.*;
import com.example.pimobiles4.util.LeitorFotoPedido;
import com.google.android.gms.tasks.Task;
import java.io.File;
import java.io.IOException;
public class FormularioOrdemActivity extends BaseActivity {
    private OrdemServico ordem; private FirebaseOrdemRepository repo;
    private boolean formularioCarregado;
    private EditText titulo,cliente,telefone,email,descricao,responsavel; private Spinner prioridade;
    private TextView importarFoto;
    private View tirarFoto;
    private File capturaPendente;
    private LeituraFoto leituraFoto;

    public static final class LeituraFoto extends ViewModel {
        private Task<String> tarefa;
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> escolherFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::lerFoto);
    private final ActivityResultLauncher<Uri> capturarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), this::receberCaptura);
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);if(isFinishing())return;tela(R.layout.activity_formulario);
        repo=new FirebaseOrdemRepository();
        titulo=findViewById(R.id.titulo);cliente=findViewById(R.id.cliente);telefone=findViewById(R.id.telefone);
        email=findViewById(R.id.emailCliente);descricao=findViewById(R.id.descricao);responsavel=findViewById(R.id.responsavel);
        prioridade=findViewById(R.id.prioridade);prioridade.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Prioridade.values()));
        long id=getIntent().getLongExtra("ordemId",0);
        findViewById(R.id.cancelar).setOnClickListener(v -> finish());
        findViewById(R.id.salvar).setOnClickListener(v -> salvar());
        if (id == 0) {
            ordem = new OrdemServico();
            prioridade.setSelection(Prioridade.MEDIA.ordinal());
            configurarImportacaoFoto();
            if (state != null && state.getString("capturaPendente") != null) {
                capturaPendente = new File(diretorioFotos(), state.getString("capturaPendente"));
            }
            if (state == null) {
                preencherRascunho(getIntent().getStringExtra(RascunhoPedido.EXTRA_TEXTO));
            } else if (state.getBoolean("rascunhoPreenchido")) {
                findViewById(R.id.revisarRascunho).setVisibility(View.VISIBLE);
            }
            return;
        }
        ((TextView) findViewById(R.id.cabecalho)).setText("Carregando OS #" + id + "…");
        habilitarCampos(false);
        repo.buscar(id).addOnCompleteListener(resultado -> {
            if (isDestroyed() || isFinishing()) return;
            if (!resultado.isSuccessful()) {
                aviso(resultado.getException());
                finish();
                return;
            }
            ordem = resultado.getResult();
            if (ordem == null) {
                Toast.makeText(this, "Ordem não encontrada", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            ((TextView) findViewById(R.id.cabecalho)).setText("Editar OS #" + id);
            if (state == null || !state.getBoolean("formularioCarregado")) preencher();
            formularioCarregado = true;
            habilitarCampos(true);
        });
    }

    private void configurarImportacaoFoto() {
        leituraFoto = new ViewModelProvider(this).get(LeituraFoto.class);
        importarFoto = findViewById(R.id.importarFotoPedido);
        tirarFoto = findViewById(R.id.tirarFotoPedido);
        tirarFoto.setVisibility(View.VISIBLE);
        tirarFoto.setOnClickListener(v -> abrirCamera());
        importarFoto.setVisibility(View.VISIBLE);
        importarFoto.setOnClickListener(v -> escolherFoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
    }

    private File diretorioFotos() {
        return new File(getCacheDir(), "fotos-pedido");
    }

    private Uri uriCaptura(File arquivo) {
        return FileProvider.getUriForFile(this, getPackageName() + ".fotos", arquivo);
    }

    private void abrirCamera() {
        if (capturaPendente != null || leituraFoto.tarefa != null) return;
        try {
            File pasta = diretorioFotos();
            if (!pasta.isDirectory() && !pasta.mkdirs()) throw new IOException("Pasta indisponível");
            capturaPendente = File.createTempFile("pedido-", ".jpg", pasta);
            capturarFoto.launch(uriCaptura(capturaPendente));
        } catch (IOException | ActivityNotFoundException | SecurityException erro) {
            if (capturaPendente != null) capturaPendente.delete();
            capturaPendente = null;
            Toast.makeText(this, R.string.camera_pedido_falha, Toast.LENGTH_LONG).show();
        }
    }

    private void receberCaptura(boolean confirmada) {
        File arquivo = capturaPendente;
        capturaPendente = null;
        if (arquivo == null) return;
        if (!confirmada || leituraFoto == null || leituraFoto.tarefa != null) {
            arquivo.delete();
            return;
        }
        lerFoto(uriCaptura(arquivo));
        leituraFoto.tarefa.addOnCompleteListener(resultado -> arquivo.delete());
    }

    @Override protected void onStart() {
        super.onStart();
        if (leituraFoto != null && leituraFoto.tarefa != null) acompanharFoto();
    }

    private void lerFoto(Uri foto) {
        if (foto == null || leituraFoto == null || leituraFoto.tarefa != null) return;
        leituraFoto.tarefa = LeitorFotoPedido.ler(this, foto);
        acompanharFoto();
    }

    private void acompanharFoto() {
        importarFoto.setText(R.string.lendo_foto);
        importarFoto.setEnabled(false);
        tirarFoto.setEnabled(false);
        findViewById(R.id.salvar).setEnabled(false);
        Task<String> tarefa = leituraFoto.tarefa;
        tarefa.addOnCompleteListener(this, resultado -> {
            if (leituraFoto.tarefa != tarefa || isFinishing()) return;
            leituraFoto.tarefa = null;
            importarFoto.setText(R.string.importar_foto);
            importarFoto.setEnabled(true);
            tirarFoto.setEnabled(true);
            findViewById(R.id.salvar).setEnabled(true);
            if (!resultado.isSuccessful()) {
                Toast.makeText(this, R.string.foto_falha, Toast.LENGTH_LONG).show();
            } else if (!preencherRascunho(resultado.getResult())) {
                Toast.makeText(this, R.string.foto_sem_texto, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean preencherRascunho(String texto) {
        RascunhoPedido rascunho = RascunhoPedido.aPartirDoTexto(texto);
        if (rascunho.descricao.isEmpty()) return false;
        preencherSeVazio(titulo, rascunho.titulo);
        preencherSeVazio(cliente, rascunho.cliente);
        preencherSeVazio(telefone, rascunho.telefone);
        preencherSeVazio(email, rascunho.email);
        preencherSeVazio(descricao, rascunho.descricao);
        findViewById(R.id.revisarRascunho).setVisibility(View.VISIBLE);
        return true;
    }

    private static void preencherSeVazio(EditText campo, String sugestao) {
        if (campo.getText().toString().trim().isEmpty() && !sugestao.isEmpty()) campo.setText(sugestao);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (capturaPendente != null) state.putString("capturaPendente", capturaPendente.getName());
        state.putBoolean("formularioCarregado", formularioCarregado);
        View revisao = findViewById(R.id.revisarRascunho);
        state.putBoolean("rascunhoPreenchido", revisao != null && revisao.getVisibility() == View.VISIBLE);
    }

    private void preencher() {
        titulo.setText(ordem.getTitulo());
        cliente.setText(ordem.getCliente().getNome());
        telefone.setText(ordem.getCliente().getTelefone());
        email.setText(ordem.getCliente().getEmail());
        descricao.setText(ordem.getDescricao());
        responsavel.setText(ordem.getResponsavel());
        prioridade.setSelection(ordem.getPrioridade().ordinal());
    }

    private void habilitarCampos(boolean habilitados) {
        titulo.setEnabled(habilitados);
        cliente.setEnabled(habilitados);
        telefone.setEnabled(habilitados);
        email.setEnabled(habilitados);
        descricao.setEnabled(habilitados);
        responsavel.setEnabled(habilitados);
        prioridade.setEnabled(habilitados);
        if (importarFoto != null) importarFoto.setEnabled(habilitados);
        if (tirarFoto != null) tirarFoto.setEnabled(habilitados);
        findViewById(R.id.salvar).setEnabled(habilitados);
    }
    private String valor(EditText v){return v.getText().toString().trim();}
    private boolean obrigatorio(EditText v){if(valor(v).isEmpty()){v.setError("Campo obrigatório");v.requestFocus();return false;}return true;}
    private void salvar(){
        if(!obrigatorio(titulo)||!obrigatorio(cliente)||!obrigatorio(descricao))return;
        if(!valor(email).isEmpty()&&!Patterns.EMAIL_ADDRESS.matcher(valor(email)).matches()){email.setError("E-mail inválido");email.requestFocus();return;}
        Cliente c=new Cliente();c.setNome(valor(cliente));c.setTelefone(valor(telefone));c.setEmail(valor(email));
        ordem.setCliente(c);ordem.setTitulo(valor(titulo));ordem.setDescricao(valor(descricao));ordem.setResponsavel(valor(responsavel));ordem.setPrioridade((Prioridade)prioridade.getSelectedItem());
        TextView botao = findViewById(R.id.salvar);
        habilitarCampos(false);
        botao.setText("Salvando…");
        repo.salvar(ordem).addOnCompleteListener(resultado -> {
            if (isDestroyed() || isFinishing()) return;
            habilitarCampos(true);
            botao.setText("Salvar ordem");
            if (resultado.isSuccessful()) {
                Toast.makeText(this, "Ordem salva", Toast.LENGTH_SHORT).show();
                finish();
            } else aviso(resultado.getException());
        });
    }

    @Override protected void onPostCreate(Bundle state) {
        super.onPostCreate(state);
        if (!isFinishing() && getIntent().getLongExtra("ordemId", 0) == 0) {
            SugestoesOrdem.configurar(this, repo);
        }
    }
}
