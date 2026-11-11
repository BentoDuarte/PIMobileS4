package com.example.pimobiles4.util;

import android.content.Context;
import android.net.Uri;

import com.example.pimobiles4.model.RascunhoPedido;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

public final class LeitorPdfPedido {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_PAGINAS = 10;

    private LeitorPdfPedido() {}

    public static String ler(Context contexto, Uri uri) throws IOException {
        byte[] dados;
        try (InputStream entrada = contexto.getContentResolver().openInputStream(uri)) {
            if (entrada == null) throw new IOException("PDF indisponível");
            ByteArrayOutputStream arquivo = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = entrada.read(buffer)) != -1) {
                if (arquivo.size() + lidos > MAX_BYTES) throw new IOException("PDF grande demais");
                arquivo.write(buffer, 0, lidos);
            }
            dados = arquivo.toByteArray();
        }
        PDFBoxResourceLoader.init(contexto);
        MemoryUsageSetting memoria = MemoryUsageSetting.setupMixed(4L * 1024 * 1024, 32L * 1024 * 1024)
                .setTempDir(contexto.getCacheDir());
        try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(dados), memoria)) {
            if (pdf.getNumberOfPages() > MAX_PAGINAS) throw new IOException("PDF com muitas páginas");
            PDFTextStripper extrator = new PDFTextStripper();
            extrator.setSortByPosition(true);
            TextoLimitado texto = new TextoLimitado();
            extrator.writeText(pdf, texto);
            return RascunhoPedido.limitarTexto(texto.conteudo);
        }
    }

    private static final class TextoLimitado extends Writer {
        private final StringBuilder conteudo = new StringBuilder();

        @Override public void write(char[] buffer, int inicio, int tamanho) {
            int restante = RascunhoPedido.MAX_TEXTO - conteudo.length();
            conteudo.append(buffer, inicio, Math.min(tamanho, restante));
        }

        @Override public void flush() {}

        @Override public void close() {}
    }
}
