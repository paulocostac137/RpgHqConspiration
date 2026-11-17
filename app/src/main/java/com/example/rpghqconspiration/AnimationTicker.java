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

    private final Runnable ciclo = new Runnable() {

        @Override
        public void run() {

            atualizacao.run();

            handler.postDelayed(
                    this,
                    intervalo
            );
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

        handler.post(ciclo);
    }

    public void parar() {

        handler.removeCallbacks(ciclo);
    }
}