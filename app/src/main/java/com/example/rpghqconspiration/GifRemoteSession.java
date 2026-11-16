package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

public final class GifRemoteSession {

    private static final String TAG = "GifRemoteSession";

    public interface Callback {
        void onStatusUpdated(String mensagem);
        void onPauseAnimation();
        void onResumeAnimationIfAllowed();
        void onRequestRenderFrame();
        void onApplyRemoteProgress(float progressoFinal);
        void onEnviarComandoControle(boolean ativo, float progresso);
    }

    private final Callback callback;
    private final GifSessionState gifSessionState;
    private final GifPlayer gifPlayer;

    public GifRemoteSession(Callback callback,
                            GifSessionState gifSessionState,
                            GifPlayer gifPlayer) {
        this.callback = callback;
        this.gifSessionState = gifSessionState;
        this.gifPlayer = gifPlayer;
    }

    public void processarMensagemServidor(JSONObject dados) {
        if (dados == null) return;

        String tipo = dados.optString("type");
        Log.d(TAG, "Processando mensagem: " + dados);

        switch (tipo) {
            case "joined": {
                int usuarios = dados.optInt("users", 0);
                callback.onStatusUpdated(
                        usuarios == 1
                                ? "Conectado. Aguardando parceiro..."
                                : "Conectado! Dois usuários na sala."
                );
                break;
            }

            case "peer_connected": {
                callback.onStatusUpdated("PARCEIRO CONECTADO!");
                Log.d(TAG, "O parceiro entrou na sala.");
                break;
            }

            case "peer_disconnected": {
                if (gifSessionState != null) {
                    gifSessionState.setRemotoAtivo(false);
                }

                if (gifPlayer != null &&
                        gifPlayer.getMovie() != null &&
                        (gifSessionState == null || !gifSessionState.isManualAtivo())) {
                    callback.onResumeAnimationIfAllowed();
                }

                callback.onStatusUpdated("Parceiro desconectado.");
                break;
            }

            case "error": {
                callback.onStatusUpdated(
                        "Erro do servidor: " + dados.optString("message")
                );
                break;
            }

            case "control": {
                processarControleRemoto(dados);
                break;
            }

            default: {
                Log.d(TAG, "Mensagem não reconhecida: " + tipo);
                break;
            }
        }
    }

    public void processarControleRemoto(JSONObject dados) {
        if (dados == null) return;

        boolean ativo = dados.optBoolean("active", false);

        if (!ativo) {
            if (gifSessionState != null) {
                gifSessionState.setRemotoAtivo(false);
            }

            Log.d(TAG, "Parceiro liberou os controles.");

            if (gifPlayer != null &&
                    gifPlayer.getMovie() != null &&
                    (gifSessionState == null || !gifSessionState.isManualAtivo())) {
                callback.onResumeAnimationIfAllowed();
            }
            return;
        }

        if (gifPlayer == null || gifPlayer.getMovie() == null) {
            return;
        }

        float progressoRecebido = (float) dados.optDouble("progress", 0);
        progressoRecebido = Math.max(0f, Math.min(1f, progressoRecebido));

        if (gifSessionState != null) {
            gifSessionState.setRemotoAtivo(true);
        }

        callback.onPauseAnimation();

        int duracao = gifPlayer.getMovie().duration();
        if (duracao <= 0) {
            duracao = 1000;
        }

        gifPlayer.setPosicaoMs((long) (progressoRecebido * duracao));
        callback.onApplyRemoteProgress(progressoRecebido);
        callback.onRequestRenderFrame();
    }

    public void enviarComandoControle(boolean ativo, float progresso) {
        if (callback != null) {
            callback.onEnviarComandoControle(ativo, progresso);
        }
    }
}
