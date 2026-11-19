package com.example.rpghqconspiration;

import android.os.Handler;

/**
 * Responsável por agendar atualizações periódicas
 * da animação.
 *
 * Não controla o conteúdo dos quadros nem o GIF.
 */
public class AnimationTicker {

    private final Handler handler;
    private final Runnable atualizacao;
    private final long intervalo;
    private boolean ativo;

    private final Runnable ciclo = new Runnable() {

        @Override
        public void run() {
            if (!ativo) {
                return;
            }

            atualizacao.run();

            if (ativo) {
                handler.postDelayed(this, intervalo);
            }
        }
    };

    public AnimationTicker(
            Handler handler,
            Runnable atualizacao,
            long intervalo) {

        this.handler = handler;
        this.atualizacao = atualizacao;
        this.intervalo = intervalo;
    }

    public void iniciar() {

        parar();

        ativo = true;
        handler.post(ciclo);
    }

    public void parar() {

        ativo = false;
        handler.removeCallbacks(ciclo);
    }
}