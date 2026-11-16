package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

public final class SharedConnectionSession {

    private static final String TAG = "SharedConnectionSession";

    public interface Callback {
        void onConectado();
        void onDesconectado(String motivo);
        void onMensagem(JSONObject mensagem);
        void onErro(String erro);
    }

    private final Callback callback;

    public SharedConnectionSession(Callback callback) {
        this.callback = callback;
    }

    public ServicoConexao bindAndGet() {
        ServicoConexao conexao = ConexaoActivity.conexao;

        if (conexao == null) {
            Log.w(TAG, "Nenhuma conexão compartilhada disponível.");
            return null;
        }

        conexao.definirCallback(new ServicoConexao.Callback() {
            @Override
            public void onConectado() {
                if (callback != null) {
                    callback.onConectado();
                }
            }

            @Override
            public void onDesconectado(String motivo) {
                if (callback != null) {
                    callback.onDesconectado(motivo);
                }
            }

            @Override
            public void onMensagem(JSONObject mensagem) {
                if (callback != null) {
                    callback.onMensagem(mensagem);
                }
            }

            @Override
            public void onErro(String erro) {
                if (callback != null) {
                    callback.onErro(erro);
                }
            }
        });

        return conexao;
    }

    public boolean isConnected() {
        ServicoConexao conexao = ConexaoActivity.conexao;
        return conexao != null && conexao.estaConectado();
    }
}
