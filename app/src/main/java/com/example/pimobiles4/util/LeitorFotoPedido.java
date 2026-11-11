package com.example.pimobiles4.util;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class LeitorFotoPedido {
    private static final Executor LEITURA = Executors.newSingleThreadExecutor();

    private LeitorFotoPedido() {}

    public static Task<String> ler(Context contexto, Uri foto) {
        Context app = contexto.getApplicationContext();
        return Tasks.call(LEITURA, () -> InputImage.fromFilePath(app, foto))
                .onSuccessTask(LEITURA, imagem -> {
                    TextRecognizer leitor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                    try {
                        return leitor.process(imagem).continueWith(LEITURA, resultado -> {
                            try {
                                if (!resultado.isSuccessful()) throw resultado.getException();
                                return resultado.getResult().getText();
                            } finally {
                                leitor.close();
                            }
                        });
                    } catch (RuntimeException erro) {
                        leitor.close();
                        throw erro;
                    }
                });
    }
}
