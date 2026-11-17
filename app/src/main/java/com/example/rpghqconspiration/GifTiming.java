package com.example.rpghqconspiration;

import android.os.Handler;
import android.os.SystemClock;

public final class GifTiming {

    public interface Callback {
        void onAutoAdvanceRequested();
        void onTextVisibilityChanged(boolean visible);
    }

    private final Handler handler;
    private final long intervaloGifMs;
    private final long tempoTextoMs;
    private final long intervaloAtualizacaoMs;
    private final Callback callback;

    private long inicioExibicaoGif = 0L;
    private boolean trocaAutomaticaAtiva = false;
    private boolean trocaAutomaticaPausada = false;
    private long proximaTrocaEm = 0L;
    private long tempoRestantePausado = 0L;

    private final Runnable trocaAutomaticaRunnable = new Runnable() {
        @Override
        public void run() {
            if (callback != null) {
                callback.onAutoAdvanceRequested();
            }

            if (trocaAutomaticaAtiva && !trocaAutomaticaPausada) {
                proximaTrocaEm =
                        SystemClock.uptimeMillis() + intervaloGifMs;
                handler.postDelayed(this, intervaloGifMs);
            }
        }
    };

    private final Runnable atualizacaoTextoRunnable = new Runnable() {
        @Override
        public void run() {
            if (inicioExibicaoGif == 0L) {
                if (callback != null) {
                    callback.onTextVisibilityChanged(false);
                }
                return;
            }

            long decorrido = SystemClock.uptimeMillis() - inicioExibicaoGif;
            boolean visivel = decorrido >= tempoTextoMs && decorrido < intervaloGifMs;

            if (callback != null) {
                callback.onTextVisibilityChanged(visivel);
            }

            if (decorrido < intervaloGifMs) {
                handler.postDelayed(this, intervaloAtualizacaoMs);
            }
        }
    };

    public GifTiming(Handler handler,
                     long intervaloGifMs,
                     long tempoTextoMs,
                     long intervaloAtualizacaoMs,
                     Callback callback) {
        this.handler = handler;
        this.intervaloGifMs = intervaloGifMs;
        this.tempoTextoMs = tempoTextoMs;
        this.intervaloAtualizacaoMs = intervaloAtualizacaoMs;
        this.callback = callback;
    }

    public void iniciarTrocaAutomatica() {
        pararTrocaAutomatica();
        trocaAutomaticaAtiva = true;
        trocaAutomaticaPausada = false;
        proximaTrocaEm =
                SystemClock.uptimeMillis() + intervaloGifMs;
        handler.postDelayed(trocaAutomaticaRunnable, intervaloGifMs);
    }

    public void pararTrocaAutomatica() {
        trocaAutomaticaAtiva = false;
        trocaAutomaticaPausada = false;
        tempoRestantePausado = 0L;
        handler.removeCallbacks(trocaAutomaticaRunnable);
    }

    public void pausarTrocaAutomatica() {
        if (!trocaAutomaticaAtiva || trocaAutomaticaPausada) {
            return;
        }

        tempoRestantePausado = Math.max(
                0L,
                proximaTrocaEm - SystemClock.uptimeMillis()
        );
        trocaAutomaticaPausada = true;
        handler.removeCallbacks(trocaAutomaticaRunnable);
    }

    public void retomarTrocaAutomatica() {
        if (!trocaAutomaticaAtiva || !trocaAutomaticaPausada) {
            return;
        }

        trocaAutomaticaPausada = false;
        proximaTrocaEm =
                SystemClock.uptimeMillis() + tempoRestantePausado;
        handler.postDelayed(
                trocaAutomaticaRunnable,
                tempoRestantePausado
        );
        tempoRestantePausado = 0L;
    }

    public void marcarInicioExibicao() {
        inicioExibicaoGif = SystemClock.uptimeMillis();
        handler.removeCallbacks(atualizacaoTextoRunnable);
        handler.post(atualizacaoTextoRunnable);
    }

    public void resetarExibicao() {
        inicioExibicaoGif = 0L;
        handler.removeCallbacks(atualizacaoTextoRunnable);
        if (callback != null) {
            callback.onTextVisibilityChanged(false);
        }
    }

    public boolean hasExibicaoAtiva() {
        return inicioExibicaoGif != 0L;
    }

    public void limpar() {
        pararTrocaAutomatica();
        handler.removeCallbacks(atualizacaoTextoRunnable);
        resetarExibicao();
    }
}
