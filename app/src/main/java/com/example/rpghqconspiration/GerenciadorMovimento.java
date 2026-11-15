package com.example.rpghqconspiration;

import android.util.Log;

/**
 * Gerencia a contagem dos ciclos de movimentação
 * e decide o que fazer quando os controles são liberados.
 *
 * Não controla diretamente Views, MotionEvents ou GIFs.
 */
public final class GerenciadorMovimento {

    private static final String TAG = "GerenciadorMovimento";

    // =====================================================
    // CONFIGURAÇÕES
    // =====================================================

    private static final int CICLOS_NECESSARIOS = 3;

    private static final float LIMITE_EXTREMO_SUPERIOR = 0.15f;

    private static final float LIMITE_EXTREMO_INFERIOR = 0.85f;

    // =====================================================
    // ESTADO DA MOVIMENTAÇÃO
    // =====================================================

    private int ciclosConcluidos = 0;

    private int extremoInicial = -1;

    private boolean alcancouExtremoOposto = false;

    // =====================================================
    // COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        boolean possuiGif();

        void avancarParaProximoGif();

        void retomarAnimacao();
    }

    private final Listener listener;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorMovimento(Listener listener) {

        if (listener == null) {
            throw new IllegalArgumentException(
                    "O Listener não pode ser nulo."
            );
        }

        this.listener = listener;
    }

    // =====================================================
    // REGISTRAR PROGRESSO DO MOVIMENTO
    // =====================================================

    public void registrarProgresso(
            float progresso,
            boolean controleManualAtivo,
            boolean controleRemotoAtivo) {

        if (!controleManualAtivo &&
                !controleRemotoAtivo) {

            return;
        }

        if (Float.isNaN(progresso) ||
                Float.isInfinite(progresso)) {

            return;
        }

        progresso = Math.max(
                0f,
                Math.min(1f, progresso)
        );

        int extremoAtual = -1;

        if (progresso <= LIMITE_EXTREMO_SUPERIOR) {

            extremoAtual = 0;

        } else if (progresso >= LIMITE_EXTREMO_INFERIOR) {

            extremoAtual = 1;
        }

        if (extremoAtual == -1) {
            return;
        }

        if (extremoInicial == -1) {

            extremoInicial = extremoAtual;

            return;
        }

        if (!alcancouExtremoOposto &&
                extremoAtual != extremoInicial) {

            alcancouExtremoOposto = true;

            return;
        }

        if (alcancouExtremoOposto &&
                extremoAtual == extremoInicial) {

            ciclosConcluidos++;

            alcancouExtremoOposto = false;

            Log.d(
                    TAG,
                    "Ciclo completo: " +
                            ciclosConcluidos +
                            "/" +
                            CICLOS_NECESSARIOS
            );
        }
    }

    // =====================================================
    // REINICIAR CONTAGEM
    // =====================================================

    public void reiniciarContagem() {

        ciclosConcluidos = 0;

        extremoInicial = -1;

        alcancouExtremoOposto = false;

        Log.d(
                TAG,
                "Contagem de movimentos reiniciada."
        );
    }

    // =====================================================
    // FINALIZAR MOVIMENTO
    // =====================================================

    public void finalizarMovimentoSeLiberado(
            boolean esquerdoPressionado,
            boolean direitoPressionado,
            boolean controleManualAtivo,
            boolean controleRemotoAtivo) {

        // Ainda existe algum controle em utilização.

        if (esquerdoPressionado ||
                direitoPressionado ||
                controleManualAtivo ||
                controleRemotoAtivo) {

            return;
        }

        // Não existe GIF carregado.

        if (!listener.possuiGif()) {

            reiniciarContagem();

            return;
        }

        // Movimento suficiente para avançar.

        if (ciclosConcluidos >= CICLOS_NECESSARIOS) {

            Log.d(
                    TAG,
                    "Três ciclos concluídos. Avançando para o próximo GIF."
            );

            reiniciarContagem();

            listener.avancarParaProximoGif();

            return;
        }

        // Movimento insuficiente: mantém o GIF atual.

        Log.d(
                TAG,
                "Movimento insuficiente: " +
                        ciclosConcluidos +
                        "/" +
                        CICLOS_NECESSARIOS +
                        ". Mantendo o GIF atual."
        );

        reiniciarContagem();

        listener.retomarAnimacao();
    }

    // =====================================================
    // CONSULTA DO PROGRESSO
    // =====================================================

    public int getCiclosConcluidos() {

        return ciclosConcluidos;
    }

    public int getCiclosNecessarios() {

        return CICLOS_NECESSARIOS;
    }
}