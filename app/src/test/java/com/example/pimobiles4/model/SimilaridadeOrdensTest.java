package com.example.pimobiles4.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimilaridadeOrdensTest {
    @Test public void encontraSinonomosEAcentosSemIncluirAssuntosDiferentes() {
        OrdemServico relacionada = ordem(1, "Quedas de conexão na recepção");
        OrdemServico outra = ordem(2, "Impressora sem toner na recepção");
        assertEquals(Collections.singletonList(relacionada),
                SimilaridadeOrdens.buscar("Wi-Fi cai na recepcao", "", Arrays.asList(outra, relacionada)));
    }

    @Test public void limitaSugestoesEPriorizaMaisRecentesNoEmpate() {
        OrdemServico a = ordem(1, "Wi-Fi recepção");
        OrdemServico b = ordem(2, "Rede recepção");
        OrdemServico c = ordem(3, "Rede recepção");
        OrdemServico d = ordem(4, "Internet recepção");
        List<OrdemServico> sugestoes = SimilaridadeOrdens.buscar("Wi-Fi recepção", "", Arrays.asList(a, b, c, d));
        assertEquals(Arrays.asList(d, c, b), sugestoes);
        assertTrue(SimilaridadeOrdens.buscar("rede", "", sugestoes).isEmpty());
    }

    @Test public void encontraTituloParecidoMesmoComDescricaoLonga() {
        OrdemServico relacionada = ordem(1, "Quedas de conexão na recepção");
        String descricao = "Começou ontem pela manhã após a troca do equipamento. "
                + "As pessoas próximas da porta relataram episódios frequentes.";
        assertEquals(Collections.singletonList(relacionada), SimilaridadeOrdens.buscar(
                "Wi-Fi cai na recepção", descricao, Collections.singletonList(relacionada)));
    }

    private OrdemServico ordem(long id, String titulo) {
        OrdemServico ordem = new OrdemServico();
        ordem.setId(id);
        ordem.setTitulo(titulo);
        return ordem;
    }
}
