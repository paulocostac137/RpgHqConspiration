package com.example.rpghqconspiration;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Gerencia a visibilidade dos controles laterais
 * conforme o modo de execução e a conexão.
 *
 * Não manipula movimentos nem comunicação WebSocket.
 */
public final class GerenciadorEstadoControles {

    private static final String TAG =
            "GerenciadorEstadoControles";

    private final TextView controleEsquerdo;
    private final TextView controleDireito;

    private boolean modoIndividual;

    public GerenciadorEstadoControles(
            TextView controleEsquerdo,
            TextView controleDireito) {

        if (controleEsquerdo == null ||
                controleDireito == null) {

            throw new IllegalArgumentException(
                    "Os controles laterais são obrigatórios."
            );
        }

        this.controleEsquerdo = controleEsquerdo;
        this.controleDireito = controleDireito;
    }

    /**
     * Define se o aplicativo funciona sem servidor.
     */
    public void definirModoIndividual(boolean modoIndividual) {

        this.modoIndividual = modoIndividual;
    }

    /**
     * O protocolo do servidor não atribui papéis nem envia
     * eventos de desbloqueio; ambos os participantes podem controlar.
     */
    public void atualizar(ServicoConexao servicoConexao) {

        if (controleEsquerdo == null ||
                controleDireito == null) {

            return;
        }

        boolean mostrarControles = modoIndividual ||
                (servicoConexao != null && servicoConexao.estaConectado());

        definirVisibilidade(
                mostrarControles ? View.VISIBLE : View.INVISIBLE
        );

        Log.d(TAG, "Controles visíveis: " + mostrarControles);
    }

    private void definirVisibilidade(int visibilidade) {

        controleEsquerdo.setVisibility(visibilidade);

        controleDireito.setVisibility(visibilidade);
    }
}