package com.example.pimobiles4;

import com.example.pimobiles4.model.Prioridade;
import com.example.pimobiles4.model.StatusOrdemServico;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleUnitTest {
    @Test
    public void statusEmAndamentoPodeSerLidoDosDadosSalvos() {
        StatusOrdemServico status = StatusOrdemServico.valueOf("EM_ANDAMENTO");

        assertEquals(StatusOrdemServico.EM_ANDAMENTO, status);
        assertEquals("Em andamento", status.toString());
    }

    @Test
    public void prioridadeMediaExibeDescricaoAcentuada() {
        assertEquals("Média", Prioridade.MEDIA.toString());
    }

    @Test
    public void prioridadeAltaMantemChaveDePersistencia() {
        assertEquals("ALTA", Prioridade.ALTA.name());
    }
}
