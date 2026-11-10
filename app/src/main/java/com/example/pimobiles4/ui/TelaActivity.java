package com.example.pimobiles4.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pimobiles4.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class TelaActivity extends AppCompatActivity {
    private static final String PREFERENCIAS = "acessibilidade";
    private static final String TEXTO_AMPLIADO = "textoAmpliado";
    private static final String ALTO_CONTRASTE = "altoContraste";

    private boolean textoAmpliado;
    private boolean altoContraste;

    @Override protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
        textoAmpliado = prefs.getBoolean(TEXTO_AMPLIADO, false);
        altoContraste = prefs.getBoolean(ALTO_CONTRASTE, false);
        if (textoAmpliado) {
            Configuration configuracao = new Configuration();
            configuracao.fontScale = base.getResources().getConfiguration().fontScale * 1.2f;
            base = base.createConfigurationContext(configuracao);
        }
        super.attachBaseContext(base);
    }

    @Override protected void onCreate(Bundle state) {
        if (altoContraste) setTheme(R.style.Theme_PIMobileS4_Contraste);
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!isFinishing() && preferenciasMudaram()) recreate();
    }

    protected void tela(int layout) {
        EdgeToEdge.enable(this);
        setContentView(R.layout.estrutura_tela);
        FrameLayout conteudo = findViewById(R.id.conteudoTela);
        getLayoutInflater().inflate(layout, conteudo, true);
        View raiz = findViewById(R.id.estruturaTela);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (view, insets) -> {
            Insets margem = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(margem.left, margem.top, margem.right, margem.bottom);
            return insets;
        });
        View engrenagem = findViewById(R.id.menuAcessibilidade);
        TooltipCompat.setTooltipText(engrenagem, getString(R.string.acessibilidade));
        engrenagem.setOnClickListener(v -> abrirAcessibilidade());
    }

    private void abrirAcessibilidade() {
        boolean[] selecionadas = {textoAmpliado, altoContraste};
        String[] opcoes = {getString(R.string.texto_ampliado), getString(R.string.alto_contraste)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.acessibilidade)
                .setMultiChoiceItems(opcoes, selecionadas,
                        (dialogo, indice, marcada) -> selecionadas[indice] = marcada)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aplicar, (dialogo, botao) -> {
                    preferencias().edit()
                            .putBoolean(TEXTO_AMPLIADO, selecionadas[0])
                            .putBoolean(ALTO_CONTRASTE, selecionadas[1])
                            .apply();
                    if (preferenciasMudaram()) recreate();
                })
                .show();
    }

    private SharedPreferences preferencias() {
        return getSharedPreferences(PREFERENCIAS, MODE_PRIVATE);
    }

    private boolean preferenciasMudaram() {
        SharedPreferences prefs = preferencias();
        return textoAmpliado != prefs.getBoolean(TEXTO_AMPLIADO, false)
                || altoContraste != prefs.getBoolean(ALTO_CONTRASTE, false);
    }
}
