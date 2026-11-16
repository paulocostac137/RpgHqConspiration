package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;

public final class GifMediaFlow {

    private static final String TAG = "GifMediaFlow";

    public interface Callback {
        void onStatusChanged(String mensagem);
        void onBotaoProximoEnabled(boolean enabled);
        void onTextoPlanilhaChanged(String texto);
        void onRequestDesenharQuadro();
        void onRequestIniciarAnimacao();
        void onRequestPausarAnimacao();
        void onRequestResetarControles();
        void onRequestCentralizarControles();
    }

    private final Handler handler;
    private final ExecutorService executor;
    private final File pastaGifs;
    private final GifCatalog catalog;
    private final GifPlayer gifPlayer;
    private final GifTiming gifTiming;
    private final GifSessionState gifSessionState;
    private final Callback callback;

    public GifMediaFlow(Handler handler,
                        ExecutorService executor,
                        File pastaGifs,
                        GifCatalog catalog,
                        GifPlayer gifPlayer,
                        GifTiming gifTiming,
                        GifSessionState gifSessionState,
                        Callback callback) {
        this.handler = handler;
        this.executor = executor;
        this.pastaGifs = pastaGifs;
        this.catalog = catalog;
        this.gifPlayer = gifPlayer;
        this.gifTiming = gifTiming;
        this.gifSessionState = gifSessionState;
        this.callback = callback;
    }

    public void proximo() {
        if (gifSessionState.isCarregando()) {
            return;
        }

        if (catalog == null || catalog.isEmpty()) {
            callback.onStatusChanged("Nenhum item disponível.");
            return;
        }

        int indiceAtual = catalog.proximoIndice();

        if (indiceAtual < 0) {
            callback.onStatusChanged("Nenhum item disponível.");
            return;
        }

        baixarEExibir(indiceAtual);
    }

    public void baixarEExibir(int posicao) {
        String nome = catalog.getNome(posicao);
        String link = catalog.getLink(posicao);

        String id = new DriveGifRepository().extrairId(link);

        if (id == null) {
            callback.onStatusChanged("Link inválido: " + nome);
            if (gifTiming != null) {
                gifTiming.iniciarTrocaAutomatica();
            }
            return;
        }

        File arquivo = new File(pastaGifs, id + ".gif");

        if (arquivo.exists() && arquivo.length() > 0) {
            exibirGif(arquivo, nome);
            return;
        }

        gifSessionState.setCarregando(true);
        callback.onBotaoProximoEnabled(false);
        callback.onStatusChanged("Baixando: " + nome);

        DriveGifRepository.baixarSeNecessario(
                pastaGifs,
                link,
                handler,
                executor,
                new DriveGifRepository.Callback() {
                    @Override
                    public void onSuccess(File arquivoBaixado) {
                        gifSessionState.setCarregando(false);
                        callback.onBotaoProximoEnabled(true);
                        exibirGif(arquivoBaixado, nome);
                    }

                    @Override
                    public void onError(String mensagem, Exception erro) {
                        gifSessionState.setCarregando(false);
                        callback.onBotaoProximoEnabled(true);
                        callback.onStatusChanged(mensagem);
                        if (gifTiming != null) {
                            gifTiming.iniciarTrocaAutomatica();
                        }
                    }
                }
        );
    }

    public void exibirGif(File arquivo, String nome) {
        if (gifTiming != null) {
            gifTiming.pararTrocaAutomatica();
            gifTiming.resetarExibicao();
        }

        if (gifPlayer != null) {
            gifPlayer.pausar();
        }

        callback.onTextoPlanilhaChanged("");
        callback.onRequestResetarControles();
        callback.onRequestCentralizarControles();

        executor.execute(() -> {
            try {
                if (gifPlayer != null) {
                    gifPlayer.carregar(arquivo);
                }

                handler.post(() -> {
                    if (gifPlayer != null) {
                        gifPlayer.setPosicaoMs(0);
                    }

                    String texto = catalog.getTexto(catalog.getIndiceAtual());
                    callback.onTextoPlanilhaChanged(texto);

                    callback.onRequestDesenharQuadro();
                    callback.onRequestIniciarAnimacao();

                    if (gifTiming != null) {
                        gifTiming.marcarInicioExibicao();
                        gifTiming.iniciarTrocaAutomatica();
                    }

                    callback.onStatusChanged(
                            nome + " | " +
                                    (catalog.getIndiceAtual() + 1) +
                                    "/" + catalog.tamanho()
                    );
                });

            } catch (Exception e) {
                Log.e(TAG, "Erro ao abrir GIF.", e);

                handler.post(() -> {
                    callback.onTextoPlanilhaChanged("");
                    callback.onStatusChanged(
                            "Erro ao abrir GIF: " + e.getMessage()
                    );
                    if (gifTiming != null) {
                        gifTiming.iniciarTrocaAutomatica();
                    }
                });
            }
        });
    }
}
