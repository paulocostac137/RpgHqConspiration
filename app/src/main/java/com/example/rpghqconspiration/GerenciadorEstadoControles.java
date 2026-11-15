package com.example.rpghqconspiration;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Gerencia o estado de liberação e visibilidade
 * dos controles laterais dos usuários A e B.
 *
 * Não manipula movimentos nem comunicação WebSocket.
 */
public final class GerenciadorEstadoControles {

    private static final String TAG =
            "GerenciadorEstadoControles";

    private final TextView controleEsquerdo;
    private final TextView controleDireito;

    private boolean modoIndividual;
    private boolean usuarioBDesbloqueado;

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
     * Libera os controles do usuário B.
     */
    public void liberarUsuarioB() {

        usuarioBDesbloqueado = true;
    }

    /**
     * Consulta se o usuário B já foi liberado.
     */
    public boolean isUsuarioBDesbloqueado() {

        return usuarioBDesbloqueado;
    }

    /**
     * Atualiza a visibilidade conforme o papel
     * do usuário e o estado da sessão.
     */
    public void atualizar(ServicoConexao servicoConexao) {

        if (controleEsquerdo == null ||
                controleDireito == null) {

            return;
        }

        // No modo individual, ambos os controles ficam visíveis.

        if (modoIndividual) {

            definirVisibilidade(View.VISIBLE);

            return;
        }

        String papel = "";

        if (servicoConexao != null) {

            papel = servicoConexao.getPapelUsuario();

            if (papel == null) {
                papel = "";
            }
        }

        boolean ocultarControles =
                !"A".equalsIgnoreCase(papel) &&
                        !usuarioBDesbloqueado;

        int visibilidade = ocultarControles
                ? View.INVISIBLE
                : View.VISIBLE;

        definirVisibilidade(visibilidade);

        Log.d(
                TAG,
                "Papel: " + papel +
                        " | Usuário B liberado: " +
                        usuarioBDesbloqueado +
                        " | Controles visíveis: " +
                        !ocultarControles
        );
    }

    private void definirVisibilidade(int visibilidade) {

        controleEsquerdo.setVisibility(visibilidade);

        controleDireito.setVisibility(visibilidade);
    }
}