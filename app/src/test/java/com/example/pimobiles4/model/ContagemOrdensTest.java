package com.example.pimobiles4.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.example.pimobiles4.model.StatusOrdemServico.*;
import static org.junit.Assert.assertEquals;

public class ContagemOrdensTest {
    @Test public void contaTotalESeparaTodosOsStatus() {
        ContagemOrdens resumo = new ContagemOrdens(Arrays.asList(
                ordem(ABERTA), ordem(ABERTA), ordem(EM_ANDAMENTO), ordem(CONCLUIDA), ordem(CANCELADA)));

        assertEquals(5, resumo.total());
        assertEquals(2, resumo.porStatus(ABERTA));
        assertEquals(1, resumo.porStatus(EM_ANDAMENTO));
        assertEquals(1, resumo.porStatus(CONCLUIDA));
        assertEquals(1, resumo.porStatus(CANCELADA));
    }

    @Test public void retornaZeroQuandoNaoExistemOrdens() {
        ContagemOrdens resumo = new ContagemOrdens(Collections.emptyList());

        assertEquals(0, resumo.total());
        for (StatusOrdemServico status : StatusOrdemServico.values()) {
            assertEquals(0, resumo.porStatus(status));
        }
    }

    @Test public void incluiOrdemSemStatusApenasNoTotal() {
        ContagemOrdens resumo = new ContagemOrdens(Arrays.asList(ordem(null), ordem(ABERTA)));

        assertEquals(2, resumo.total());
        assertEquals(1, resumo.porStatus(ABERTA));
        assertEquals(0, resumo.porStatus(CONCLUIDA));
    }

    private OrdemServico ordem(StatusOrdemServico status) {
        OrdemServico ordem = new OrdemServico();
        ordem.setStatus(status);
        return ordem;
    }
}
