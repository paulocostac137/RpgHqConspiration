package com.example.rpghqconspiration;

import android.graphics.Movie;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

        void onDecodificacaoConcluida(GifFrameSequence sequencia);

        void onErro(Exception erro);
    }

    public interface PreloadListener {
        void onPreloadConcluido(GifFrameSequence sequencia);
        void onErro(Exception erro);
    }

    public interface TimelineListener {
        void onTimelineReady(GifFrameSequence timeline);
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

                    GifFrameSequence sequencia =
                            GifDecoder.carregarSequencia(arquivo);

                    if (sequencia == null) {

                        throw new IllegalStateException(
                                "Não foi possível decodificar o GIF."
                        );
                    }

                    final GifFrameSequence resultado = sequencia;

                    Log.d(
                            TAG,
                            "Decodificação concluída: " +
                                    arquivo.getName()
                    );

                    handler.post(() ->
                            listener.onDecodificacaoConcluida(
                                    resultado
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

    /**
     * Baixa o próximo GIF antecipadamente, sem interromper
     * a reprodução atual.
     */
    public void precarregar(
            String link,
            GifRepository repository) {

        if (link == null || link.trim().isEmpty() ||
                repository == null) {
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    File arquivo = repository.obter(link);

                    Log.d(
                            TAG,
                            "Arquivo do próximo GIF preparado: " +
                                    arquivo.getName()
                    );
                } catch (Exception erro) {
                    Log.w(
                            TAG,
                            "Não foi possível preparar o próximo GIF.",
                            erro
                    );
                }
            });
        } catch (RuntimeException erro) {
            Log.w(TAG, "O executor não aceitou o pré-carregamento.", erro);
        }
    }

    public void preparar(
            String link,
            GifRepository repository,
            PreloadListener listener) {

        if (link == null || link.trim().isEmpty() ||
                repository == null || listener == null) {
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    File arquivo = repository.obter(link);
                    GifFrameSequence sequencia =
                            GifDecoder.carregarSequencia(arquivo);
                    if (sequencia == null) {
                        throw new IllegalStateException(
                                "Não foi possível decodificar o GIF preparado."
                        );
                    }

                    handler.post(() ->
                            listener.onPreloadConcluido(sequencia)
                    );
                } catch (Exception erro) {
                    Log.w(TAG, "Falha ao preparar o próximo GIF.", erro);
                    handler.post(() -> listener.onErro(erro));
                }
            });
        } catch (RuntimeException erro) {
            Log.e(TAG, "Não foi possível iniciar o preparo do GIF.", erro);
            handler.post(() -> listener.onErro(erro));
        }
    }

    public void prepararLinhaDoTempo(
            List<String> links,
            GifRepository repository,
            TimelineListener listener) {
        if (links == null || links.isEmpty() ||
                repository == null || listener == null) {
            throw new IllegalArgumentException(
                    "Links, repositório e listener são obrigatórios."
            );
        }

        List<String> linksCopiados = new ArrayList<>(links);
        try {
            executor.execute(() -> {
                List<GifFrameSequence> sequencias = new ArrayList<>();
                try {
                    int limiteQuadros = Math.max(300, linksCopiados.size());
                    int limiteDimensao = Math.max(
                            1,
                            Math.min(
                                    240,
                                    (int) Math.sqrt(
                                            (32d * 1024d * 1024d)
                                                    / (2d * limiteQuadros)
                                    )
                            )
                    );
                    int quadrosUsados = 0;

                    for (int i = 0; i < linksCopiados.size(); i++) {
                        File arquivo = repository.obter(linksCopiados.get(i));
                        Movie movie = GifDecoder.carregar(arquivo);
                        int restantes = linksCopiados.size() - i;
                        int quotaQuadros = Math.max(
                                1,
                                Math.min(
                                        60,
                                        (limiteQuadros - quadrosUsados)
                                                / restantes
                                )
                        );
                        GifFrameSequence sequencia =
                                GifFrameSequence.extract(
                                        movie,
                                        quotaQuadros,
                                        limiteDimensao
                                );
                        sequencias.add(sequencia);
                        quadrosUsados += sequencia.getFrameCount();
                    }

                    GifFrameSequence timeline =
                            GifFrameSequence.concatenate(sequencias, 5);
                    handler.post(() -> listener.onTimelineReady(timeline));
                } catch (Exception erro) {
                    for (GifFrameSequence sequencia : sequencias) {
                        sequencia.liberar();
                    }
                    Log.e(TAG, "Falha ao preparar a linha do tempo dos GIFs.", erro);
                    handler.post(() -> listener.onErro(erro));
                }
            });
        } catch (RuntimeException erro) {
            Log.e(TAG, "Não foi possível iniciar a linha do tempo.", erro);
            handler.post(() -> listener.onErro(erro));
        }
    }
}