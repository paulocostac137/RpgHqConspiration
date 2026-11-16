package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

public final class ConexaoFlow {

    private static final String TAG = "ConexaoFlow";

    public interface Callback {
        void onStatusUpdated(String mensagem);
        void onLiberarTelaConexao();
        void onAbrirTelaPrincipal();
    }

    private final Callback callback;
    private boolean abriuTelaPrincipal = false;
    private boolean aguardandoParceiro = false;

    public ConexaoFlow(Callback callback) {
        this.callback = callback;
    }

    public boolean isAbriuTelaPrincipal() {
        return abriuTelaPrincipal;
    }

    public void setAbriuTelaPrincipal(boolean abriuTelaPrincipal) {
        this.abriuTelaPrincipal = abriuTelaPrincipal;
    }

    public void resetar() {
        abriuTelaPrincipal = false;
        aguardandoParceiro = false;
    }

    public void processarMensagem(JSONObject mensagem) {
        if (mensagem == null) {
            return;
        }

        String tipo = mensagem.optString("type", "");
        Log.d(TAG, "Evento recebido do servidor: " + mensagem);

        switch (tipo) {
            case "joined": {
                int usuarios = mensagem.optInt("users", 0);

                if (usuarios >= 2) {
                    abrirTelaPrincipal();
                } else {
                    aguardandoParceiro = true;
                    callback.onStatusUpdated(
                            "Etapa 3 de 3: conectado à sala; aguardando o outro participante..."
                    );
                }
                break;
            }

            case "peer_connected": {
                callback.onStatusUpdated(
                        "Participante conectado! Abrindo o aplicativo..."
                );
                abrirTelaPrincipal();
                break;
            }

            case "peer_disconnected": {
                callback.onStatusUpdated("Seu parceiro desconectou.");
                break;
            }

            case "error": {
                String erro = mensagem.optString(
                        "message",
                        "O servidor informou um erro."
                );

                callback.onStatusUpdated(erro);
                callback.onLiberarTelaConexao();
                break;
            }

            default: {
                Log.d(TAG, "Evento não tratado: " + tipo);
                break;
            }
        }
    }

    public void abrirTelaPrincipal() {
        if (abriuTelaPrincipal) {
            return;
        }

        abriuTelaPrincipal = true;
        callback.onAbrirTelaPrincipal();
    }

    public void liberarTelaConexao() {
        aguardandoParceiro = false;
        callback.onLiberarTelaConexao();
    }
}
