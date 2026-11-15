package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

public final class GerenciadorSessao {

    private static final String TAG = "GerenciadorSessao";

    private ServicoConexao servicoConexao;

    private Listener listener;

    private ServicoConexao.Callback callbackRegistrado;

    // =====================================================
    // INTERFACE DE COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        boolean atividadeDisponivel();

        void onConectado();

        void onDesconectado(String motivo);

        void onMensagem(JSONObject mensagem);

        void onErro(String erro);
    }

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorSessao() {
    }

    // =====================================================
    // REGISTRAR CALLBACKS
    // =====================================================

    public void registrarCallbacks(
            ServicoConexao servico,
            Listener novoListener) {

        if (servico == null || novoListener == null) {

            Log.e(TAG, "Serviço ou listener inválido.");

            return;
        }

        this.servicoConexao = servico;
        this.listener = novoListener;

        callbackRegistrado = new ServicoConexao.Callback() {

            @Override
            public void onConectado() {

                Listener atual = obterListenerAtivo();

                if (atual != null) {
                    atual.onConectado();
                }
            }

            @Override
            public void onDesconectado(String motivo) {

                Listener atual = obterListenerAtivo();

                if (atual != null) {
                    atual.onDesconectado(motivo);
                }
            }

            @Override
            public void onMensagem(JSONObject mensagem) {

                Listener atual = obterListenerAtivo();

                if (atual != null) {
                    atual.onMensagem(mensagem);
                }
            }

            @Override
            public void onErro(String erro) {

                Listener atual = obterListenerAtivo();

                if (atual != null) {
                    atual.onErro(erro);
                }
            }
        };

        servicoConexao.definirCallback(callbackRegistrado);

        Log.d(TAG, "Callbacks registrados.");
    }

    // =====================================================
    // VERIFICAR DISPONIBILIDADE DA ATIVIDADE
    // =====================================================

    private Listener obterListenerAtivo() {

        Listener atual = listener;

        if (atual == null) {
            return null;
        }

        if (!atual.atividadeDisponivel()) {
            return null;
        }

        return atual;
    }

    // =====================================================
    // REMOVER CALLBACK
    // =====================================================

    public void removerCallback() {

        if (servicoConexao != null) {

            servicoConexao.definirCallback(null);
        }

        callbackRegistrado = null;

        listener = null;

        servicoConexao = null;

        Log.d(TAG, "Callbacks removidos.");
    }
}