package com.example.pimobiles4.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.pimobiles4.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class TelaActivity extends AppCompatActivity {
    private static final String PREFERENCIAS = "acessibilidade";
    private static final String TEXTO_AMPLIADO = "textoAmpliado";
    private static final String NIVEL_TEXTO = "nivelTexto";
    private static final String ALTO_CONTRASTE = "altoContraste";
    private static final String TEMA_ESCURO = "temaEscuro";
    private static final String BOTOES_MAIORES = "botoesMaiores";
    private static final float[] ESCALAS_TEXTO = {1f, 1.1f, 1.2f, 1.4f};
    private static final int[] OPCOES_TEXTO = {
            R.id.textoPadrao, R.id.textoMais10, R.id.textoMais20, R.id.textoMais40
    };

    private int nivelTexto;
    private boolean altoContraste;
    private boolean temaEscuro;
    private boolean botoesMaiores;

    @Override protected void attachBaseContext(Context base) {
        SharedPreferences prefs = base.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
        nivelTexto = lerNivelTexto(prefs);
        altoContraste = prefs.getBoolean(ALTO_CONTRASTE, false);
        temaEscuro = prefs.getBoolean(TEMA_ESCURO, false);
        botoesMaiores = prefs.getBoolean(BOTOES_MAIORES, false);
        if (nivelTexto != 0) {
            Configuration configuracao = new Configuration();
            configuracao.fontScale = base.getResources().getConfiguration().fontScale
                    * ESCALAS_TEXTO[nivelTexto];
            base = base.createConfigurationContext(configuracao);
        }
        super.attachBaseContext(base);
    }

    @Override protected void onCreate(Bundle state) {
        if (temaEscuro) {
            setTheme(altoContraste ? R.style.Theme_PIMobileS4_Escuro_Contraste
                    : R.style.Theme_PIMobileS4_Escuro);
        } else if (altoContraste) {
            setTheme(R.style.Theme_PIMobileS4_Contraste);
        }
        if (botoesMaiores) getTheme().applyStyle(R.style.Overlay_PIMobileS4_ToqueAmpliado, true);
        super.onCreate(state);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!isFinishing() && preferenciasMudaram()) recreate();
    }

    protected void tela(int layout) {
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat barras = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barras.setAppearanceLightStatusBars(!temaEscuro);
        barras.setAppearanceLightNavigationBars(!temaEscuro);
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

    final void abrirAcessibilidade() {
        View ajustes = getLayoutInflater().inflate(R.layout.dialogo_acessibilidade, null);
        RadioGroup tamanhos = ajustes.findViewById(R.id.tamanhosTexto);
        CompoundButton contraste = ajustes.findViewById(R.id.preferenciaAltoContraste);
        CompoundButton escuro = ajustes.findViewById(R.id.preferenciaTemaEscuro);
        CompoundButton maiores = ajustes.findViewById(R.id.preferenciaBotoesMaiores);
        tamanhos.check(OPCOES_TEXTO[nivelTexto]);
        contraste.setChecked(altoContraste);
        escuro.setChecked(temaEscuro);
        maiores.setChecked(botoesMaiores);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.acessibilidade)
                .setView(ajustes)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.aplicar, (dialogo, botao) -> {
                    int nivelSelecionado = nivelTexto;
                    for (int i = 0; i < OPCOES_TEXTO.length; i++) {
                        if (tamanhos.getCheckedRadioButtonId() == OPCOES_TEXTO[i]) {
                            nivelSelecionado = i;
                            break;
                        }
                    }
                    preferencias().edit()
                            .putInt(NIVEL_TEXTO, nivelSelecionado)
                            .remove(TEXTO_AMPLIADO)
                            .putBoolean(ALTO_CONTRASTE, contraste.isChecked())
                            .putBoolean(TEMA_ESCURO, escuro.isChecked())
                            .putBoolean(BOTOES_MAIORES, maiores.isChecked())
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
        return nivelTexto != lerNivelTexto(prefs)
                || altoContraste != prefs.getBoolean(ALTO_CONTRASTE, false)
                || temaEscuro != prefs.getBoolean(TEMA_ESCURO, false)
                || botoesMaiores != prefs.getBoolean(BOTOES_MAIORES, false);
    }

    private static int lerNivelTexto(SharedPreferences prefs) {
        int nivel = prefs.getInt(NIVEL_TEXTO, prefs.getBoolean(TEXTO_AMPLIADO, false) ? 2 : 0);
        return nivel >= 0 && nivel < ESCALAS_TEXTO.length ? nivel : 0;
    }
}
