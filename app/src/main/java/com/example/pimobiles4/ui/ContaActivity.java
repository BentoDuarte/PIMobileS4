package com.example.pimobiles4.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pimobiles4.R;
import com.example.pimobiles4.model.RascunhoPedido;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class ContaActivity extends TelaActivity {
    public static final String EXTRA_CADASTRO = "cadastro";
    public static final String EXTRA_EMAIL = "email";

    public static class Envio extends ViewModel {
        private Task<?> tarefa;
    }

    private boolean cadastro;
    private EditText email;
    private EditText senha;
    private EditText confirmacao;
    private TextView enviar;
    private TextView mensagem;
    private Envio envio;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        cadastro = getIntent().getBooleanExtra(EXTRA_CADASTRO, false);
        envio = new ViewModelProvider(this).get(Envio.class);
        tela(R.layout.activity_conta);

        email = findViewById(R.id.emailConta);
        senha = findViewById(R.id.novaSenha);
        confirmacao = findViewById(R.id.confirmarSenha);
        enviar = findViewById(R.id.enviarConta);
        mensagem = findViewById(R.id.mensagemConta);
        TextView titulo = findViewById(R.id.tituloConta);
        TextView instrucoes = findViewById(R.id.instrucoesConta);
        titulo.setText(cadastro ? "Criar conta" : "Recuperar senha");
        instrucoes.setText(cadastro ? "Informe seu e-mail e escolha uma senha."
                : "Informe o e-mail da sua conta para receber o link de redefinição.");
        if (state == null) email.setText(getIntent().getStringExtra(EXTRA_EMAIL));

        if (!cadastro) {
            findViewById(R.id.grupoSenha).setVisibility(View.GONE);
            findViewById(R.id.grupoConfirmacao).setVisibility(View.GONE);
        }
        email.setImeOptions(cadastro ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        EditText ultimoCampo = cadastro ? confirmacao : email;
        ultimoCampo.setOnEditorActionListener((campo, acao, evento) -> {
            if (acao != EditorInfo.IME_ACTION_DONE) return false;
            enviar.performClick();
            return true;
        });
        findViewById(R.id.voltarLogin).setOnClickListener(v -> finish());
        enviar.setOnClickListener(v -> enviar());
        atualizarEnvio();
    }

    @Override protected void onStart() {
        super.onStart();
        if (envio.tarefa != null) acompanharEnvio();
    }

    private void enviar() {
        if (envio.tarefa != null) return;
        mensagem.setVisibility(View.GONE);
        String endereco = email.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(endereco).matches()) {
            email.setError("Informe um e-mail válido");
            email.requestFocus();
            return;
        }

        String senhaDigitada = senha.getText().toString();
        if (cadastro) {
            if (senhaDigitada.length() < 6) {
                senha.setError("Use pelo menos 6 caracteres");
                senha.requestFocus();
                return;
            }
            if (!senhaDigitada.equals(confirmacao.getText().toString())) {
                confirmacao.setError("As senhas precisam ser iguais");
                confirmacao.requestFocus();
                return;
            }
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            mostrarMensagem("O Firebase ainda precisa ser configurado.");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (cadastro) {
            envio.tarefa = auth.createUserWithEmailAndPassword(endereco, senhaDigitada);
        } else {
            auth.setLanguageCode("pt-BR");
            envio.tarefa = auth.sendPasswordResetEmail(endereco);
        }
        atualizarEnvio();
        acompanharEnvio();
    }

    private void acompanharEnvio() {
        envio.tarefa.addOnCompleteListener(this, resultado -> {
            envio.tarefa = null;
            atualizarEnvio();
            if (cadastro && resultado.isSuccessful()) {
                Intent painel = new Intent(this, ListaOrdensActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                String texto = RascunhoPedido.limitarTexto(getIntent().getStringExtra(RascunhoPedido.EXTRA_TEXTO));
                if (texto.isEmpty()) {
                    startActivity(painel);
                } else {
                    Intent formulario = new Intent(this, FormularioOrdemActivity.class)
                            .putExtra(RascunhoPedido.EXTRA_TEXTO, texto);
                    startActivities(new Intent[]{painel, formulario});
                }
                return;
            }
            Exception erro = resultado.getException();
            if (!cadastro && (resultado.isSuccessful()
                    || erro instanceof FirebaseAuthInvalidUserException)) {
                mostrarMensagem("Se esse e-mail estiver cadastrado, você receberá um link. "
                        + "Confira a caixa de entrada e o spam.");
            } else if (erro instanceof FirebaseAuthWeakPasswordException) {
                mostrarMensagem("A senha não atende aos requisitos. Escolha uma senha mais forte.");
            } else if (erro instanceof FirebaseAuthUserCollisionException) {
                mostrarMensagem("Não foi possível criar a conta. Se você já se cadastrou, entre ou recupere a senha.");
            } else if (erro instanceof FirebaseNetworkException) {
                mostrarMensagem("Confira a conexão com a internet e tente novamente.");
            } else if (erro instanceof FirebaseTooManyRequestsException) {
                mostrarMensagem("Muitas tentativas. Aguarde um pouco e tente novamente.");
            } else {
                mostrarMensagem(cadastro ? "Não foi possível criar a conta. Tente novamente."
                        : "Não foi possível solicitar o link. Tente novamente.");
            }
        });
    }

    private void atualizarEnvio() {
        boolean aguardando = envio.tarefa != null;
        email.setEnabled(!aguardando);
        senha.setEnabled(!aguardando);
        confirmacao.setEnabled(!aguardando);
        enviar.setEnabled(!aguardando);
        enviar.setText(aguardando ? "Enviando…" : cadastro ? "Criar conta" : "Enviar link");
    }

    private void mostrarMensagem(String texto) {
        mensagem.setText(texto);
        mensagem.setVisibility(View.VISIBLE);
    }
}
