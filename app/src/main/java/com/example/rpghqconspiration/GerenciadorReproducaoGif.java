package com.example.rpghqconspiration;

import android.graphics.Movie;
import android.os.Handler;
import android.os.SystemClock;

/**
 * Responsável pelo controle temporal da reprodução dos GIFs.
 *
 * Controla:
 * - início e pausa da reprodução;
 * - posição atual da animação;
 * - atualização periódica dos quadros.
 *
 * Não manipula Views nem decide como o quadro será desenhado.
 */
public final class GerenciadorReproducaoGif {

    public interface Listener {

        /**
         * Informa se a reprodução pode ser iniciada.
         */
        boolean podeIniciar();

        /**
         * Solicita à MainActivity que desenhe o quadro atual.
         */
        void atualizarQuadro();
    }

    private final AnimationTicker animationTicker;
    private final Listener listener;

    private long posicaoMs = 0;
    private long inicioReproducao = 0;

    private boolean reproduzindo = false;

    public GerenciadorReproducaoGif(
            Handler handler,
            long intervalo,
            Listener listener) {

        if (handler == null || listener == null) {
            throw new IllegalArgumentException(
                    "Handler e Listener são obrigatórios."
            );
        }

        this.listener = listener;

        animationTicker = new AnimationTicker(
                handler,
                this::atualizar,
                intervalo
        );
    }

    private void atualizar() {

        if (!reproduzindo) {
            return;
        }

        long agora = SystemClock.uptimeMillis();

        long decorrido = agora - inicioReproducao;

        posicaoMs += decorrido;

        inicioReproducao = agora;

        listener.atualizarQuadro();
    }

    public void iniciar(Movie movie) {

        if (movie == null ||
                reproduzindo ||
                !listener.podeIniciar()) {

            return;
        }

        reproduzindo = true;

        inicioReproducao =
                SystemClock.uptimeMillis();

        animationTicker.iniciar();
    }

    public void pausar(Movie movie) {

        if (movie == null) {
            return;
        }

        if (reproduzindo) {

            int duracao = obterDuracao(movie);

            long agora = SystemClock.uptimeMillis();

            long decorrido = agora - inicioReproducao;

            posicaoMs = (
                    posicaoMs + decorrido
            ) % duracao;
        }

        reproduzindo = false;

        animationTicker.parar();
    }

    public int obterDuracao(Movie movie) {

        if (movie == null) {
            return 1000;
        }

        int duracao = movie.duration();

        return duracao > 0 ? duracao : 1000;
    }

    public long getPosicaoMs() {
        return posicaoMs;
    }

    public void definirPosicao(long posicaoMs) {

        this.posicaoMs = Math.max(0, posicaoMs);
    }

    public boolean isReproduzindo() {
        return reproduzindo;
    }

    public void reiniciar() {

        animationTicker.parar();

        reproduzindo = false;

        posicaoMs = 0;

        inicioReproducao = 0;
    }

    public void encerrar() {

        animationTicker.parar();

        reproduzindo = false;

        inicioReproducao = 0;
    }
}