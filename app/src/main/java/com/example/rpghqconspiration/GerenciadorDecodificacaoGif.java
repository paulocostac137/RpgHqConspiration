package com.example.rpghqconspiration;

import android.graphics.Movie;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Responsável exclusivamente pela decodificação
 * assíncrona dos arquivos GIF.
 *
 * Não manipula Views.
 * Não controla a reprodução.
 * Não altera o estado da MainActivity.
 */
public final class GerenciadorDecodificacaoGif {

    private static final String TAG =
            "GerenciadorDecodificacaoGif";

    public interface Listener {

        void onDecodificacaoConcluida(Movie movie);

        void onErro(Exception erro);
    }

    private final ExecutorService executor;
    private final Handler handler;

    public GerenciadorDecodificacaoGif(
            ExecutorService executor,
            Handler handler) {

        if (executor == null || handler == null) {

            throw new IllegalArgumentException(
                    "As dependências não podem ser nulas."
            );
        }

        this.executor = executor;
        this.handler = handler;
    }

    // =====================================================
    // DECODIFICAR GIF
    // =====================================================

    public void decodificar(
            File arquivo,
            Listener listener) {

        if (listener == null) {
            throw new IllegalArgumentException(
                    "O Listener é obrigatório."
            );
        }

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() <= 0) {

            handler.post(() ->
                    listener.onErro(
                            new IllegalArgumentException(
                                    "Arquivo GIF inválido."
                            )
                    )
            );

            return;
        }

        try {

            executor.execute(() -> {

                try {

                    Log.d(
                            TAG,
                            "Iniciando decodificação: " +
                                    arquivo.getName()
                    );

                    Movie movie =
                            GifDecoder.carregar(arquivo);

                    if (movie == null) {

                        throw new IllegalStateException(
                                "Não foi possível decodificar o GIF."
                        );
                    }

                    Log.d(
                            TAG,
                            "Decodificação concluída: " +
                                    arquivo.getName()
                    );

                    handler.post(() ->
                            listener.onDecodificacaoConcluida(
                                    movie
                            )
                    );

                } catch (Exception erro) {

                    Log.e(
                            TAG,
                            "Falha na decodificação do GIF.",
                            erro
                    );

                    handler.post(() ->
                            listener.onErro(erro)
                    );
                }
            });

        } catch (RuntimeException erro) {

            Log.e(
                    TAG,
                    "Não foi possível iniciar a decodificação.",
                    erro
            );

            handler.post(() ->
                    listener.onErro(erro)
            );
        }
    }
}