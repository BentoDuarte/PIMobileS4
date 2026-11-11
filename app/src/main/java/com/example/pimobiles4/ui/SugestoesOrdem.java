package com.example.pimobiles4.ui;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pimobiles4.R;
import com.example.pimobiles4.data.FirebaseOrdemRepository;
import com.example.pimobiles4.model.OrdemServico;
import com.example.pimobiles4.model.SimilaridadeOrdens;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

final class SugestoesOrdem {
    private SugestoesOrdem() {}

    static void configurar(FormularioOrdemActivity tela, FirebaseOrdemRepository repo) {
        TextView botao = tela.findViewById(R.id.buscarOrdensParecidas);
        EditText titulo = tela.findViewById(R.id.titulo);
        EditText descricao = tela.findViewById(R.id.descricao);
        View salvar = tela.findViewById(R.id.salvar);
        botao.setVisibility(View.VISIBLE);
        botao.setOnClickListener(v -> {
            if (!salvar.isEnabled()) return;
            String tituloConsultado = titulo.getText().toString();
            String descricaoConsultada = descricao.getText().toString();
            if (!SimilaridadeOrdens.temTermosSuficientes(tituloConsultado + "\n" + descricaoConsultada)) {
                Toast.makeText(tela, R.string.sugestoes_pouco_texto, Toast.LENGTH_SHORT).show();
                return;
            }
            botao.setEnabled(false);
            botao.setText(R.string.sugestoes_buscando);
            repo.listar().addOnCompleteListener(resultado -> {
                if (tela.isDestroyed() || tela.isFinishing()) return;
                botao.setEnabled(true);
                botao.setText(R.string.buscar_ordens_parecidas);
                if (!tela.hasWindowFocus() || !salvar.isEnabled()
                        || !tituloConsultado.contentEquals(titulo.getText())
                        || !descricaoConsultada.contentEquals(descricao.getText())) return;
                if (!resultado.isSuccessful()) {
                    tela.aviso(resultado.getException());
                    return;
                }
                mostrar(tela, SimilaridadeOrdens.buscar(tituloConsultado, descricaoConsultada, resultado.getResult()));
            });
        });
    }

    private static void mostrar(FormularioOrdemActivity tela, List<OrdemServico> sugestoes) {
        if (sugestoes.isEmpty()) {
            Toast.makeText(tela, R.string.sugestoes_vazias, Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence[] rotulos = new CharSequence[sugestoes.size()];
        for (int i = 0; i < sugestoes.size(); i++) {
            OrdemServico ordem = sugestoes.get(i);
            rotulos[i] = tela.getString(R.string.sugestoes_item,
                    ordem.getId(), ordem.getTitulo(), ordem.getStatus());
        }
        new MaterialAlertDialogBuilder(tela)
                .setTitle(R.string.ordens_parecidas)
                .setItems(rotulos, (dialogo, posicao) -> tela.startActivity(
                        new Intent(tela, DetalhesOrdemActivity.class)
                                .putExtra("ordemId", sugestoes.get(posicao).getId())))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
