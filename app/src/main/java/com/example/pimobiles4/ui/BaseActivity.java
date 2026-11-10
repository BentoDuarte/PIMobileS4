package com.example.pimobiles4.ui;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.pimobiles4.MainActivity;
import com.example.pimobiles4.R;
import com.google.firebase.firestore.FirebaseFirestoreException;
public abstract class BaseActivity extends TelaActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(com.google.firebase.FirebaseApp.getApps(this).isEmpty()
                || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        }
    }
    protected String mensagemDeErro(Exception erro) {
        if (erro instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) erro).getCode()) {
                case PERMISSION_DENIED:
                    return "Sem permissão para acessar as ordens. Confira as regras do Firestore.";
                case UNAUTHENTICATED:
                    return "Sessão inválida. Saia e entre novamente.";
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    return "Não foi possível conectar ao banco. Confira a internet e tente novamente.";
                case NOT_FOUND:
                    return "A ordem não foi encontrada.";
                default:
                    return "Não foi possível concluir a operação no banco.";
            }
        }
        if (erro instanceof java.io.IOException && erro.getMessage() != null) {
            return erro.getMessage();
        }
        return "Não foi possível concluir a operação.";
    }

    protected void aviso(Exception erro) {
        Toast.makeText(this, mensagemDeErro(erro), Toast.LENGTH_LONG).show();
    }

    protected void confirmarExclusao(long id, Runnable excluir) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir OS #" + id + "?")
                .setMessage("A ordem será removida da sua conta.")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton("Excluir", (dialogo, botao) -> excluir.run())
                .show();
    }
}
