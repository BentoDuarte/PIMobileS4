package com.example.pimobiles4.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.IntentCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.pimobiles4.MainActivity;
import com.example.pimobiles4.R;
import com.example.pimobiles4.model.RascunhoPedido;
import com.example.pimobiles4.util.LeitorPdfPedido;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ReceberPedidoActivity extends TelaActivity {
    private static final Executor LEITURA = Executors.newSingleThreadExecutor();
    private LeituraPedido leitura;

    public static final class LeituraPedido extends ViewModel {
        private Task<String> tarefa;
    }

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        tela(R.layout.activity_receber_pedido);
        leitura = new ViewModelProvider(this).get(LeituraPedido.class);
        if (leitura.tarefa == null) {
            Context app = getApplicationContext();
            Intent pedido = getIntent();
            leitura.tarefa = Tasks.call(LEITURA, () -> lerPedido(app, pedido));
        }
    }

    @Override protected void onStart() {
        super.onStart();
        leitura.tarefa.addOnCompleteListener(this, resultado -> {
            if (isFinishing()) return;
            if (!resultado.isSuccessful()) {
                Toast.makeText(this, R.string.pedido_compartilhado_falha, Toast.LENGTH_LONG).show();
            } else if (resultado.getResult().isEmpty()) {
                Toast.makeText(this, R.string.pedido_compartilhado_sem_texto, Toast.LENGTH_LONG).show();
            } else {
                boolean autenticado = !FirebaseApp.getApps(this).isEmpty()
                        && FirebaseAuth.getInstance().getCurrentUser() != null;
                Intent destino = autenticado ? new Intent(this, FormularioOrdemActivity.class)
                        : new Intent(this, MainActivity.class);
                startActivity(destino.putExtra(RascunhoPedido.EXTRA_TEXTO, resultado.getResult()));
            }
            finish();
        });
    }

    private static String lerPedido(Context contexto, Intent pedido) throws IOException {
        if (!Intent.ACTION_SEND.equals(pedido.getAction())) throw new IOException("Ação indisponível");
        if ("text/plain".equals(pedido.getType())) {
            return RascunhoPedido.limitarTexto(pedido.getCharSequenceExtra(Intent.EXTRA_TEXT));
        }
        if ("application/pdf".equals(pedido.getType())) {
            Uri uri = IntentCompat.getParcelableExtra(pedido, Intent.EXTRA_STREAM, Uri.class);
            if (uri != null && "content".equals(uri.getScheme())) return LeitorPdfPedido.ler(contexto, uri);
        }
        throw new IOException("Formato indisponível");
    }
}
