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
    private static final long LIMITE_MEMORIA_QUADROS_BYTES =
            32L * 1024L * 1024L;
    private static final int DIMENSAO_MAXIMA_QUADRO = 240;
    private static final long LIMITE_QUADROS_LINHA_DO_TEMPO = 1_000_000L;

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
                    List<File> arquivos = new ArrayList<>(linksCopiados.size());
                    List<GifFrameInfo> metadados =
                            new ArrayList<>(linksCopiados.size());
                    long totalPosicoes = 0L;
                    long totalBitmaps = 0L;

                    for (String link : linksCopiados) {
                        File arquivo = repository.obter(link);
                        GifFrameInfo frameInfo = GifFrameInfo.read(arquivo);
                        int quantidadeQuadros =
                                frameInfo.getFrameCountAt30Fps();
                        if (quantidadeQuadros >
                                LIMITE_QUADROS_LINHA_DO_TEMPO
                                        - totalPosicoes) {
                            throw new IllegalArgumentException(
                                    "A linha do tempo ultrapassa o limite de um milhão de posições."
                            );
                        }
                        totalPosicoes += quantidadeQuadros;
                        totalBitmaps += Math.min(
                                frameInfo.getFrameCount(),
                                quantidadeQuadros
                        );
                        arquivos.add(arquivo);
                        metadados.add(frameInfo);
                    }

                    int limiteDimensao = calcularDimensaoQuadro(
                            totalBitmaps
                    );
                    for (int i = 0; i < arquivos.size(); i++) {
                        Movie movie = GifDecoder.carregar(arquivos.get(i));
                        GifFrameSequence sequencia =
                                GifFrameSequence.extract(
                                        movie,
                                        metadados.get(i),
                                        limiteDimensao
                                );
                        sequencias.add(sequencia);
                    }

                    GifFrameSequence timeline =
                            GifFrameSequence.concatenate(sequencias);
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

    private int calcularDimensaoQuadro(long totalQuadros) {
        if (totalQuadros <= 0L) {
            throw new IllegalArgumentException(
                    "A linha do tempo não contém quadros."
            );
        }
        long pixelsPorQuadro = Math.max(
                1L,
                LIMITE_MEMORIA_QUADROS_BYTES /
                        Math.max(2L, totalQuadros * 2L)
        );
        return Math.max(
                1,
                Math.min(
                        DIMENSAO_MAXIMA_QUADRO,
                        (int) Math.sqrt(pixelsPorQuadro)
                )
        );
    }
}