package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

/**
 * Responsável pelo processamento e envio dos comandos
 * de controle remoto entre os usuários A e B.
 *
 * Não manipula Views, GIFs ou movimentos diretamente.
 * A aplicação dos comandos fica sob responsabilidade
 * da MainActivity, através do Listener.
 */
public final class GerenciadorControleRemoto {

    private static final String TAG =
            "GerenciadorControleRemoto";

    public interface Listener {

        void onControleRecebido(float progresso);

        void onControleLiberado();
    }

    private final Listener listener;

    public GerenciadorControleRemoto(Listener listener) {

        if (listener == null) {
            throw new IllegalArgumentException(
                    "Listener não pode ser nulo."
            );
        }

        this.listener = listener;
    }

    /**
     * Interpreta um comando recebido do parceiro.
     */
    public void processar(JSONObject dados) {

        if (dados == null) {
            return;
        }

        boolean ativo = dados.optBoolean(
                "active",
                false
        );

        if (!ativo) {

            Log.d(
                    TAG,
                    "Parceiro liberou os controles."
            );

            listener.onControleLiberado();

            return;
        }

        float progresso = (float) dados.optDouble(
                "progress",
                0
        );

        if (Float.isNaN(progresso) ||
                Float.isInfinite(progresso)) {

            Log.w(
                    TAG,
                    "Progresso recebido inválido."
            );

            return;
        }

        progresso = Math.max(
                0f,
                Math.min(1f, progresso)
        );

        listener.onControleRecebido(progresso);
    }

    /**
     * Encaminha um comando ao parceiro.
     */
    public void enviar(
            ServicoConexao servicoConexao,
            boolean modoIndividual,
            boolean ativo,
            float progresso) {

        if (modoIndividual) {
            return;
        }

        if (servicoConexao == null) {

            Log.w(
                    TAG,
                    "Comando não enviado: serviço inexistente."
            );

            return;
        }

        if (!servicoConexao.estaConectado()) {

            Log.w(
                    TAG,
                    "Comando não enviado: conexão indisponível."
            );

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

        servicoConexao.enviarControle(
                ativo,
                progresso
        );

        Log.d(
                TAG,
                "Controle enviado. Ativo: " +
                        ativo +
                        " | Progresso: " +
                        progresso
        );
    }
}