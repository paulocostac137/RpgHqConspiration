package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * Responsável por coordenar a obtenção dos GIFs.
 *
 * Utiliza o GifRepository para localizar ou baixar
 * os arquivos, sem manipular Views ou reproduzir animações.
 */
public final class GerenciadorCarregamentoGif {

    private static final String TAG =
            "GerenciadorCarregamentoGif";

    // =====================================================
    // LISTENER
    // =====================================================

    public interface Listener {

        void onCarregamentoIniciado();

        void onCarregamentoConcluido(
                File arquivo
        );

        void onErro(Exception erro);
    }

    // =====================================================
    // DEPENDÊNCIAS
    // =====================================================

    private final GifRepository gifRepository;

    private final ExecutorService executor;

    private final Handler handler;

    private final Listener listener;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorCarregamentoGif(
            GifRepository gifRepository,
            ExecutorService executor,
            Handler handler,
            Listener listener) {

        if (gifRepository == null ||
                executor == null ||
                handler == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "As dependências não podem ser nulas."
            );
        }

        this.gifRepository = gifRepository;
        this.executor = executor;
        this.handler = handler;
        this.listener = listener;
    }

    // =====================================================
    // SOLICITAR GIF
    // =====================================================

    public void carregar(String link) {

        if (link == null || link.trim().isEmpty()) {

            handler.post(() ->
                    listener.onErro(
                            new IllegalArgumentException(
                                    "O link do GIF está vazio."
                            )
                    )
            );

            return;
        }

        handler.post(listener::onCarregamentoIniciado);

        executor.execute(() -> {

            try {

                Log.d(
                        TAG,
                        "Obtendo GIF: " + link
                );

                // O repositório primeiro procura
                // o arquivo local e baixa se necessário.

                File arquivo =
                        gifRepository.obter(link);

                validarArquivo(arquivo);

                Log.d(
                        TAG,
                        "GIF obtido: " +
                                arquivo.getAbsolutePath()
                );

                handler.post(() ->
                        listener.onCarregamentoConcluido(
                                arquivo
                        )
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Falha ao obter GIF.",
                        e
                );

                handler.post(() ->
                        listener.onErro(e)
                );
            }
        });
    }

    // =====================================================
    // VALIDAR ARQUIVO
    // =====================================================

    private void validarArquivo(File arquivo) {

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() <= 0) {

            throw new IllegalStateException(
                    "O arquivo GIF recebido é inválido."
            );
        }
    }
}