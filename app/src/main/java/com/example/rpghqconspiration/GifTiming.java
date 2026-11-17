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

    private boolean trocaAutomaticaAtiva = false;
    private boolean trocaAutomaticaPausada = false;
    private boolean textoJaExibido = false;
    private long proximaTrocaEm = 0L;
    private long tempoRestantePausado = 0L;

    private final Runnable atualizacaoTextoRunnable = new Runnable() {
        @Override
        public void run() {
            if (!trocaAutomaticaAtiva) {
                notificarVisibilidadeTexto(false);
                return;
            }

            long restante = obterTempoRestante();
            long decorrido = intervaloGifMs - restante;
            if (decorrido >= tempoTextoMs) {
                textoJaExibido = true;
                notificarVisibilidadeTexto(true);
            } else if (restante == 0L && !trocaAutomaticaPausada) {
                textoJaExibido = false;
                notificarVisibilidadeTexto(false);
            } else {
                notificarVisibilidadeTexto(textoJaExibido);
            }

            if (!trocaAutomaticaPausada && restante > 0L) {
                handler.postDelayed(
                        this,
                        Math.min(intervaloAtualizacaoMs, restante)
                );
            }
        }
    };

    private final Runnable trocaAutomaticaRunnable = new Runnable() {
        @Override
        public void run() {
            textoJaExibido = false;
            notificarVisibilidadeTexto(false);

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
        textoJaExibido = false;
        tempoRestantePausado = 0L;
        proximaTrocaEm =
                SystemClock.uptimeMillis() + intervaloGifMs;
        handler.postDelayed(trocaAutomaticaRunnable, intervaloGifMs);
        atualizarVisibilidadeTexto();
    }

    public void pararTrocaAutomatica() {
        trocaAutomaticaAtiva = false;
        trocaAutomaticaPausada = false;
        textoJaExibido = false;
        tempoRestantePausado = 0L;
        handler.removeCallbacks(trocaAutomaticaRunnable);
        handler.removeCallbacks(atualizacaoTextoRunnable);
        notificarVisibilidadeTexto(false);
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
        handler.removeCallbacks(atualizacaoTextoRunnable);
        atualizarVisibilidadeTexto();
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
        atualizarVisibilidadeTexto();
    }

    public void definirProgressoExibicao(float progresso) {
        if (!trocaAutomaticaAtiva ||
                Float.isNaN(progresso) ||
                Float.isInfinite(progresso)) {
            return;
        }

        progresso = Math.max(0f, Math.min(1f, progresso));
        if (!trocaAutomaticaPausada) {
            pausarTrocaAutomatica();
        }

        tempoRestantePausado = Math.round(
                intervaloGifMs * (1f - progresso)
        );
        atualizarVisibilidadeTexto();
    }

    public void marcarInicioExibicao() {
        atualizarVisibilidadeTexto();
    }

    public void resetarExibicao() {
        handler.removeCallbacks(atualizacaoTextoRunnable);
        notificarVisibilidadeTexto(false);
    }

    public boolean hasExibicaoAtiva() {
        return trocaAutomaticaAtiva;
    }

    public void limpar() {
        pararTrocaAutomatica();
        resetarExibicao();
    }

    private long obterTempoRestante() {
        if (trocaAutomaticaPausada) {
            return tempoRestantePausado;
        }
        return Math.max(
                0L,
                proximaTrocaEm - SystemClock.uptimeMillis()
        );
    }

    private void atualizarVisibilidadeTexto() {
        handler.removeCallbacks(atualizacaoTextoRunnable);
        handler.post(atualizacaoTextoRunnable);
    }

    private void notificarVisibilidadeTexto(boolean visible) {
        if (callback != null) {
            callback.onTextVisibilityChanged(visible);
        }
    }
}
