package com.example.pimobiles4.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RascunhoPedidoTest {
    @Test public void extraiCamposRotuladosEPreservaDescricaoMultilinha() {
        RascunhoPedido pedido = RascunhoPedido.aPartirDoTexto(
                "Assunto: Wi-Fi instável\r\nCliente: Ana Silva\r\n"
                        + "E-mail: ana@example.com\r\nTelefone: (11) 90000-0000\r\n"
                        + "Descrição: A rede cai na recepção.\r\nAcontece de manhã.");
        assertEquals("Wi-Fi instável", pedido.titulo);
        assertEquals("Ana Silva", pedido.cliente);
        assertEquals("ana@example.com", pedido.email);
        assertEquals("(11) 90000-0000", pedido.telefone);
        assertEquals("A rede cai na recepção.\nAcontece de manhã.", pedido.descricao);
    }

    @Test public void mantemTextoLivreSemInventarCliente() {
        RascunhoPedido pedido = RascunhoPedido.aPartirDoTexto("Impressora travada\nFica no segundo andar.");
        assertEquals("Impressora travada", pedido.titulo);
        assertEquals("", pedido.cliente);
        assertEquals("Impressora travada\nFica no segundo andar.", pedido.descricao);
    }
}
