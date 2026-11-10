package com.example.pimobiles4.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.pimobiles4.MainActivity;
import com.example.pimobiles4.R;
import com.example.pimobiles4.data.FirebaseOrdemRepository;
import com.example.pimobiles4.model.OrdemServico;
import com.example.pimobiles4.model.RegistroOrdem;
import com.example.pimobiles4.model.StatusOrdemServico;
import com.example.pimobiles4.util.OrdemPdf;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PainelAbas {
    private static final StatusOrdemServico[] STATUS = StatusOrdemServico.values();

    private final ListaOrdensActivity activity;
    private final FirebaseOrdemRepository repo;
    private final BottomNavigationView menu;
    private final Map<Integer, View> abas = new HashMap<>();
    private final TextView resumoInicio;
    private final View cartoesInicio;
    private final TextView totalInicio;
    private final TextView statusInicio;
    private final Spinner filtroRelatorio;
    private final TextView resumoRelatorio;
    private final View gerarRelatorio;
    private final TextView resumoRegistros;
    private final View recarregarRegistros;
    private final ArrayAdapter<String> adapterRegistros;
    private final ActivityResultLauncher<String> exportar;
    private final Map<Long, StatusOrdemServico> statusAtuais = new HashMap<>();
    private final Map<StatusOrdemServico, Integer> contagens = new EnumMap<>(StatusOrdemServico.class);

    private int total;
    private boolean ordensCarregadas;
    private String erroOrdens;
    private String pdfPendente;
    private List<RegistroOrdem> registros = Collections.emptyList();
    private Task<List<RegistroOrdem>> consultaRegistros;

    PainelAbas(ListaOrdensActivity activity, FirebaseOrdemRepository repo, Bundle state) {
        this.activity = activity;
        this.repo = repo;
        menu = activity.findViewById(R.id.menuInferior);
        FrameLayout container = activity.findViewById(R.id.abasPainel);
        View inicio = incluir(container, R.id.aba_inicio, R.layout.aba_inicio);
        incluir(container, R.id.aba_ordens, R.layout.activity_lista);
        View historico = incluir(container, R.id.aba_registros, R.layout.aba_registros);
        View relatorios = incluir(container, R.id.aba_relatorios, R.layout.aba_relatorios);
        View perfil = incluir(container, R.id.aba_perfil, R.layout.aba_perfil);

        resumoInicio = inicio.findViewById(R.id.resumoInicio);
        cartoesInicio = inicio.findViewById(R.id.cartoesInicio);
        totalInicio = inicio.findViewById(R.id.totalInicio);
        statusInicio = inicio.findViewById(R.id.statusInicio);
        inicio.findViewById(R.id.novaOrdemInicio).setOnClickListener(v -> activity.abrirNovaOrdem());
        inicio.findViewById(R.id.abrirOrdens).setOnClickListener(v ->
                menu.setSelectedItemId(R.id.aba_ordens));

        resumoRegistros = historico.findViewById(R.id.resumoRegistros);
        recarregarRegistros = historico.findViewById(R.id.recarregarRegistros);
        recarregarRegistros.setOnClickListener(v -> carregarRegistros());
        adapterRegistros = new ArrayAdapter<>(activity, R.layout.item_registro);
        ((ListView) historico.findViewById(R.id.listaRegistros)).setAdapter(adapterRegistros);

        filtroRelatorio = relatorios.findViewById(R.id.filtroRelatorio);
        resumoRelatorio = relatorios.findViewById(R.id.resumoRelatorio);
        gerarRelatorio = relatorios.findViewById(R.id.gerarRelatorio);
        configurarRelatorios();

        TextView email = perfil.findViewById(R.id.emailPerfil);
        email.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        perfil.findViewById(R.id.acessibilidadePerfil).setOnClickListener(v -> activity.abrirAcessibilidade());
        perfil.findViewById(R.id.sairPerfil).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            activity.startActivity(new Intent(activity, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        if (state != null) pdfPendente = state.getString("relatorioPendente");
        exportar = activity.registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                    String texto = pdfPendente;
                    pdfPendente = null;
                    atualizarRelatorio();
                    if (uri == null || texto == null) return;
                    try (OutputStream destino = activity.getContentResolver().openOutputStream(uri)) {
                        if (destino == null) throw new IOException("Não foi possível abrir o destino");
                        OrdemPdf.escreverRelatorio(texto, destino);
                        Toast.makeText(activity, "Relatório salvo", Toast.LENGTH_SHORT).show();
                    } catch (IOException erro) {
                        activity.aviso(erro);
                    }
                });

        menu.setOnItemSelectedListener(item -> {
            mostrarAba(item.getItemId());
            if (item.getItemId() == R.id.aba_registros) carregarRegistros();
            return true;
        });
        int aba = state == null ? R.id.aba_inicio : state.getInt("abaPainel", R.id.aba_inicio);
        mostrarAba(aba);
        menu.setSelectedItemId(aba);
        atualizarRelatorio();
    }

    private View incluir(FrameLayout container, int aba, int layout) {
        View view = activity.getLayoutInflater().inflate(layout, container, false);
        view.setVisibility(View.GONE);
        container.addView(view);
        abas.put(aba, view);
        return view;
    }

    private void mostrarAba(int selecionada) {
        abas.forEach((id, view) -> view.setVisibility(id == selecionada ? View.VISIBLE : View.GONE));
    }

    void salvarEstado(Bundle state) {
        state.putInt("abaPainel", menu.getSelectedItemId());
        state.putString("relatorioPendente", pdfPendente);
    }

    void carregandoOrdens() {
        ordensCarregadas = false;
        erroOrdens = null;
        cartoesInicio.setVisibility(View.GONE);
        resumoInicio.setText(R.string.resumo_carregando);
        resumoInicio.setVisibility(View.VISIBLE);
        atualizarRelatorio();
    }

    void erroAoCarregarOrdens(Exception erro) {
        erroOrdens = activity.mensagemDeErro(erro);
        resumoInicio.setText(activity.getString(R.string.resumo_falha, erroOrdens));
        atualizarRelatorio();
    }

    void mostrarOrdens(List<OrdemServico> ordens) {
        statusAtuais.clear();
        contagens.clear();
        for (OrdemServico ordem : ordens) {
            StatusOrdemServico status = ordem.getStatus();
            statusAtuais.put(ordem.getId(), status);
            if (status != null) contagens.merge(status, 1, Integer::sum);
        }
        total = ordens.size();
        ordensCarregadas = true;
        erroOrdens = null;
        totalInicio.setText(NumberFormat.getIntegerInstance().format(total));
        statusInicio.setText(contagemPorStatus());
        resumoInicio.setVisibility(View.GONE);
        cartoesInicio.setVisibility(View.VISIBLE);
        atualizarRelatorio();
        atualizarLinhasRegistros();
    }

    private void configurarRelatorios() {
        List<String> rotulos = new ArrayList<>();
        rotulos.add("Todos os status");
        for (StatusOrdemServico status : STATUS) rotulos.add(status.toString());
        ArrayAdapter<String> filtros = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rotulos);
        filtros.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filtroRelatorio.setAdapter(filtros);
        filtroRelatorio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> pai, View item, int posicao, long id) {
                atualizarRelatorio();
            }

            @Override public void onNothingSelected(AdapterView<?> pai) {}
        });
        gerarRelatorio.setOnClickListener(v -> solicitarPdf());
    }

    private StatusOrdemServico statusSelecionado() {
        int posicao = filtroRelatorio.getSelectedItemPosition();
        return posicao > 0 ? STATUS[posicao - 1] : null;
    }

    private String contagemPorStatus() {
        List<String> linhas = new ArrayList<>();
        for (StatusOrdemServico status : STATUS) {
            linhas.add(status + ": " + contagens.getOrDefault(status, 0));
        }
        return String.join("\n", linhas);
    }

    private void atualizarRelatorio() {
        gerarRelatorio.setEnabled(ordensCarregadas && pdfPendente == null);
        if (!ordensCarregadas) {
            resumoRelatorio.setText(erroOrdens == null ? "Carregando contagem…"
                    : erroOrdens + "\nAbra Ordens para tentar novamente.");
            return;
        }
        StatusOrdemServico status = statusSelecionado();
        resumoRelatorio.setText(status == null
                ? "Total de ordens: " + total + "\n\n" + contagemPorStatus()
                : status + ": " + contagens.getOrDefault(status, 0));
    }

    private void solicitarPdf() {
        if (!ordensCarregadas || pdfPendente != null) return;
        StatusOrdemServico status = statusSelecionado();
        String data = DateFormat.getDateTimeInstance().format(new Date());
        String filtro = status == null ? "Todos os status" : status.toString();
        int quantidade = status == null ? total : contagens.getOrDefault(status, 0);
        pdfPendente = "Relatório de ordens de serviço\n\nGerado em: " + data
                + "\nFiltro: " + filtro + "\nTotal de ordens: " + quantidade
                + (status == null ? "\n\n" + contagemPorStatus() : "");
        atualizarRelatorio();
        String sufixo = status == null ? "todas" : status.name().toLowerCase(Locale.ROOT);
        exportar.launch("relatorio-ordens-" + sufixo + ".pdf");
    }

    void atualizarRegistrosSeVisivel() {
        if (menu.getSelectedItemId() == R.id.aba_registros) carregarRegistros();
    }

    private void carregarRegistros() {
        recarregarRegistros.setVisibility(View.GONE);
        resumoRegistros.setText("Carregando registros…");
        Task<List<RegistroOrdem>> consulta = repo.listarRegistros();
        consultaRegistros = consulta;
        consulta.addOnCompleteListener(activity, resultado -> {
            if (consultaRegistros != consulta) return;
            consultaRegistros = null;
            if (resultado.isSuccessful()) {
                registros = resultado.getResult();
                resumoRegistros.setText(registros.isEmpty() ? "Nenhuma ação registrada ainda."
                        : "Ações mais recentes primeiro");
                atualizarLinhasRegistros();
            } else {
                resumoRegistros.setText(activity.mensagemDeErro(resultado.getException()));
                recarregarRegistros.setVisibility(View.VISIBLE);
            }
        });
    }

    private void atualizarLinhasRegistros() {
        DateFormat formato = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        List<String> linhas = new ArrayList<>();
        Set<Long> excluidas = new HashSet<>();
        for (RegistroOrdem registro : registros) {
            if (registro.getAcao() == RegistroOrdem.Acao.EXCLUSAO) {
                excluidas.add(registro.getOrdemId());
            }
        }
        for (RegistroOrdem registro : registros) {
            String data = registro.getData() == null ? "Data pendente" : formato.format(registro.getData());
            StatusOrdemServico atual = statusAtuais.get(registro.getOrdemId());
            String status;
            if (excluidas.contains(registro.getOrdemId())) {
                status = "Ordem excluída • Status na ação: " + registro.getStatus();
            } else if (ordensCarregadas && atual != null) {
                status = "Status atual: " + atual;
            } else {
                status = "Status na ação: " + registro.getStatus();
            }
            linhas.add("OS #" + registro.getOrdemId() + " • " + registro.getTitulo()
                    + "\n" + registro.getAcao() + " • " + data + "\n" + status);
        }
        adapterRegistros.setNotifyOnChange(false);
        adapterRegistros.clear();
        adapterRegistros.addAll(linhas);
        adapterRegistros.notifyDataSetChanged();
    }
}
