package com.example.rpghqconspiration;

import android.graphics.Movie;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
        prepararLinhaDoTempo(
                links,
                Collections.nCopies(links == null ? 0 : links.size(), 1),
                repository,
                listener
        );
    }

    public void prepararLinhaDoTempo(
            List<String> links,
            List<Integer> repeticoes,
            GifRepository repository,
            TimelineListener listener) {
        prepararLinhaDoTempo(
                links,
                repeticoes,
                Collections.nCopies(links == null ? 0 : links.size(), null),
                repository,
                listener
        );
    }

    public void prepararLinhaDoTempo(
            List<String> links,
            List<Integer> repeticoes,
            List<FaixaRepeticaoGif> faixasRepetidas,
            GifRepository repository,
            TimelineListener listener) {
        if (links == null || links.isEmpty() ||
                repeticoes == null || repeticoes.size() != links.size() ||
                faixasRepetidas == null ||
                faixasRepetidas.size() != links.size() ||
                repository == null || listener == null) {
            throw new IllegalArgumentException(
                    "Links, repetições, repositório e listener são obrigatórios."
            );
        }

        List<String> linksCopiados = new ArrayList<>(links);
        List<Integer> repeticoesCopiadas = new ArrayList<>(repeticoes);
        List<FaixaRepeticaoGif> faixasCopiadas =
                new ArrayList<>(faixasRepetidas);
        try {
            executor.execute(() -> {
                List<GifFrameSequence> sequencias = new ArrayList<>();
                try {
                    List<File> arquivos = new ArrayList<>(linksCopiados.size());
                    List<GifFrameInfo> metadados =
                            new ArrayList<>(linksCopiados.size());
                    long totalPosicoes = 0L;
                    long totalBitmaps = 0L;

                    for (int i = 0; i < linksCopiados.size(); i++) {
                        String link = linksCopiados.get(i);
                        int repeticao = repeticoesCopiadas.get(i);
                        if (repeticao <= 0) {
                            throw new IllegalArgumentException(
                                    "O número de repetições do GIF " +
                                            (i + 1) +
                                            " deve ser positivo."
                            );
                        }
                        File arquivo = repository.obter(link);
                        GifFrameInfo frameInfo = GifFrameInfo.read(arquivo);
                        FaixaRepeticaoGif faixa =
                                faixasCopiadas.get(i);
                        int quadrosDoCiclo = faixa == null
                                ? frameInfo.getFrameCountAt30Fps()
                                : frameInfo.getFrameCountAt30Fps(
                                        faixa,
                                        GifFrameSequence.REPETICOES_POR_FAIXA
                                );
                        long quantidadeQuadros =
                                (long) quadrosDoCiclo * repeticao;
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
                                frameInfo.getFrameCountAt30Fps()
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
                                        limiteDimensao,
                                        faixasCopiadas.get(i),
                                        GifFrameSequence.REPETICOES_POR_FAIXA
                                );
                        sequencias.add(sequencia);
                    }

                    GifFrameSequence timeline =
                            GifFrameSequence.concatenate(
                                    sequencias,
                                    toIntArray(repeticoesCopiadas)
                            );
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

    private int[] toIntArray(List<Integer> valores) {
        int[] resultado = new int[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            resultado[i] = valores.get(i);
        }
        return resultado;
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