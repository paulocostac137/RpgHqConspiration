package com.example.rpghqconspiration;

import android.os.Handler;
import android.os.SystemClock;

/** Advances through every frame in the combined GIF timeline. */
public final class GerenciadorReproducaoGif {

    public interface Listener {
        boolean podeIniciar();
        void atualizarQuadro();
    }

    private final AnimationTicker animationTicker;
    private final Listener listener;

    private GifFrameSequence sequencia;
    private float progresso;
    private int indiceQuadro;
    private int quadrosAvancados;
    private long tempoAcumuladoMs;
    private long inicioReproducao;
    private boolean reproduzindo;

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
        tempoAcumuladoMs += agora - inicioReproducao;
        inicioReproducao = agora;

        int totalQuadros = sequencia.getFrameCount();
        while (tempoAcumuladoMs >= obterDuracaoQuadro(quadrosAvancados)) {
            tempoAcumuladoMs -= obterDuracaoQuadro(quadrosAvancados);
            quadrosAvancados = (quadrosAvancados + 1) % totalQuadros;
        }

        indiceQuadro = quadrosAvancados;
        progresso = (float) indiceQuadro / totalQuadros;
        listener.atualizarQuadro();
    }

    private long obterDuracaoQuadro(int indice) {
        int totalQuadros = sequencia.getFrameCount();
        long duracaoTotal = sequencia.getDurationMs();
        long inicio = (long) indice * duracaoTotal / totalQuadros;
        long fim = (long) (indice + 1) * duracaoTotal / totalQuadros;
        return Math.max(1L, fim - inicio);
    }

    public void iniciar(GifFrameSequence sequencia) {
        if (sequencia == null ||
                reproduzindo ||
                !listener.podeIniciar()) {
            return;
        }

        if (this.sequencia != sequencia) {
            definirSequencia(sequencia);
        }
        reproduzindo = true;
        inicioReproducao = SystemClock.uptimeMillis();
        animationTicker.iniciar();
    }

    public void pausar() {
        reproduzindo = false;
        animationTicker.parar();
    }

    public void definirSequencia(GifFrameSequence sequencia) {
        this.sequencia = sequencia;
        indiceQuadro = sequencia == null
                ? 0
                : sequencia.getFrameIndex(progresso);
        quadrosAvancados = indiceQuadro;
        tempoAcumuladoMs = 0L;
    }

    public int getIndiceQuadro() {
        return indiceQuadro;
    }

    public float getProgresso() {
        return progresso;
    }

    public void definirProgresso(float progresso) {
        this.progresso = Math.max(0f, Math.min(1f, progresso));
        if (sequencia != null) {
            indiceQuadro = sequencia.getFrameIndex(this.progresso);
            quadrosAvancados = indiceQuadro;
            tempoAcumuladoMs = 0L;
        }
    }

    public void definirIndiceQuadro(int indice) {
        if (sequencia == null) {
            return;
        }

        indiceQuadro = Math.max(
                0,
                Math.min(sequencia.getFrameCount() - 1, indice)
        );
        quadrosAvancados = indiceQuadro;
        progresso = (float) indiceQuadro / sequencia.getFrameCount();
        tempoAcumuladoMs = 0L;
    }

    public boolean isReproduzindo() {
        return reproduzindo;
    }

    public void reiniciar() {
        animationTicker.parar();
        reproduzindo = false;
        progresso = 0f;
        indiceQuadro = 0;
        quadrosAvancados = 0;
        tempoAcumuladoMs = 0L;
        inicioReproducao = 0L;
        sequencia = null;
    }

    public void encerrar() {
        animationTicker.parar();
        reproduzindo = false;
        inicioReproducao = 0L;
    }
}
