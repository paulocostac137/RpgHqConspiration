package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

/**
 * Responsável por identificar e encaminhar
 * as mensagens recebidas do servidor.
 *
 * Não manipula Views nem controla diretamente
 * a animação dos GIFs.
 */
public final class GerenciadorMensagens {

    private static final String TAG = "GerenciadorMensagens";

    // =====================================================
    // COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        void onJoined(int usuarios);

        void onPeerConnected();

        void onPeerDisconnected();

        void onError(String mensagem);

        void onControl(JSONObject dados);

        void onMensagemDesconhecida(String tipo);
    }

    // =====================================================
    // LISTENER
    // =====================================================

    private final Listener listener;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorMensagens(Listener listener) {

        if (listener == null) {

            throw new IllegalArgumentException(
                    "O Listener não pode ser nulo."
            );
        }

        this.listener = listener;
    }

    // =====================================================
    // PROCESSAR MENSAGEM
    // =====================================================

    public void processar(JSONObject dados) {

        if (dados == null) {

            Log.e(
                    TAG,
                    "Mensagem recebida é nula."
            );

            return;
        }

        String tipo = dados.optString("type");

        Log.d(
                TAG,
                "Processando mensagem: " + dados
        );

        switch (tipo) {

            case "joined": {

                int usuarios = dados.optInt("users", 0);

                listener.onJoined(usuarios);

                break;
            }

            case "peer_connected": {

                listener.onPeerConnected();

                break;
            }

            case "peer_disconnected": {

                listener.onPeerDisconnected();

                break;
            }

            case "error": {

                String mensagem =
                        dados.optString("message");

                listener.onError(mensagem);

                break;
            }

            case "control": {

                listener.onControl(dados);

                break;
            }

            default: {

                Log.d(
                        TAG,
                        "Mensagem não reconhecida: " + tipo
                );

                listener.onMensagemDesconhecida(tipo);

                break;
            }
        }
    }
}