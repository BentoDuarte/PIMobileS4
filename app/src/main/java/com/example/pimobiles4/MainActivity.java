package com.example.pimobiles4;
import android.os.Bundle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.widget.EditText;
import com.example.pimobiles4.ui.ListaOrdensActivity;
import com.example.pimobiles4.ui.ContaActivity;
import com.example.pimobiles4.ui.TelaActivity;
public class MainActivity extends TelaActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (FirebaseApp.getApps(this).size() > 0 && FirebaseAuth.getInstance().getCurrentUser() != null) { abrir(); return; }
        tela(R.layout.activity_main);
        android.view.View botao = findViewById(R.id.entrar);
        android.view.View criarConta = findViewById(R.id.criarConta);
        android.view.View esqueciSenha = findViewById(R.id.esqueciSenha);
        EditText campoEmail = findViewById(R.id.email);
        criarConta.setOnClickListener(v -> abrirConta(true, campoEmail));
        esqueciSenha.setOnClickListener(v -> abrirConta(false, campoEmail));
        EditText campoSenha = findViewById(R.id.senha);
        campoSenha.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_DONE) return false;
            botao.performClick();
            return true;
        });
        botao.setOnClickListener(v->{
            EditText email=findViewById(R.id.email), senha=findViewById(R.id.senha);
            if (FirebaseApp.getApps(this).isEmpty()) {
                android.widget.Toast.makeText(this, "Configure o Firebase para entrar.", android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            String endereco = email.getText().toString().trim();
            String senhaDigitada = senha.getText().toString();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(endereco).matches()) {
                email.setError("Informe um e-mail válido");
                return;
            }
            if (senhaDigitada.isEmpty()) {
                senha.setError("Informe a senha");
                return;
            }
            botao.setEnabled(false);
            criarConta.setEnabled(false);
            esqueciSenha.setEnabled(false);
            FirebaseAuth.getInstance().signInWithEmailAndPassword(endereco, senhaDigitada)
                    .addOnCompleteListener(this, resultado -> {
                        botao.setEnabled(true);
                        criarConta.setEnabled(true);
                        esqueciSenha.setEnabled(true);
                        if (resultado.isSuccessful()) abrir();
                        else android.widget.Toast.makeText(this,
                                "Falha ao entrar. Confira as credenciais e a conexão.",
                                android.widget.Toast.LENGTH_LONG).show();
                    });
        });
    }
    private void abrirConta(boolean cadastro, EditText email) {
        startActivity(new Intent(this, ContaActivity.class)
                .putExtra(ContaActivity.EXTRA_CADASTRO, cadastro)
                .putExtra(ContaActivity.EXTRA_EMAIL, email.getText().toString().trim()));
    }

    private void abrir() {startActivity(new Intent(this,ListaOrdensActivity.class));finish();}
}
