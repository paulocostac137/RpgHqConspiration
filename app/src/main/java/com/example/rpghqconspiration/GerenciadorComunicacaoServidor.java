package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONObject;

/**
 * Coordena a comunicação recebida do servidor.
 *
 * Responsabilidades:
 * - Registrar os callbacks da conexão compartilhada.
 * - Encaminhar mensagens para o GerenciadorMensagens.
 * - Informar à MainActivity os eventos da conexão.
 * - Remover os callbacks ao encerrar a atividade.
 *
 * Não manipula Views nem controla GIFs.
 */
public final class GerenciadorComunicacaoServidor {

    private static final String TAG =
            "GerenciadorComunicacao";

    public interface Listener {

        boolean atividadeDisponivel();

        void onConectado();

        void onDesconectado(String motivo);

        void onErroConexao(String erro);
    }

    private final GerenciadorSessao gerenciadorSessao;

    private final GerenciadorMensagens gerenciadorMensagens;

    private final Listener listener;

    public GerenciadorComunicacaoServidor(
            GerenciadorMensagens gerenciadorMensagens,
            Listener listener) {

        if (gerenciadorMensagens == null || listener == null) {

            throw new IllegalArgumentException(
                    "Os gerenciadores e o Listener são obrigatórios."
            );
        }

        this.gerenciadorMensagens = gerenciadorMensagens;
        this.listener = listener;

        this.gerenciadorSessao = new GerenciadorSessao();
    }

    // =====================================================
    // REGISTRAR COMUNICAÇÃO
    // =====================================================

    public void registrarCallbacks(ServicoConexao servico) {

        if (servico == null) {

            Log.e(
                    TAG,
                    "Não foi possível registrar: serviço nulo."
            );

            return;
        }

        gerenciadorSessao.registrarCallbacks(
                servico,
                new GerenciadorSessao.Listener() {

                    @Override
                    public boolean atividadeDisponivel() {

                        return listener.atividadeDisponivel();
                    }

                    @Override
                    public void onConectado() {

                        if (listener.atividadeDisponivel()) {
                            listener.onConectado();
                        }
                    }

                    @Override
                    public void onDesconectado(String motivo) {

                        if (listener.atividadeDisponivel()) {
                            listener.onDesconectado(motivo);
                        }
                    }

                    @Override
                    public void onMensagem(JSONObject mensagem) {

                        if (!listener.atividadeDisponivel()) {
                            return;
                        }

                        gerenciadorMensagens.processar(mensagem);
                    }

                    @Override
                    public void onErro(String erro) {

                        if (listener.atividadeDisponivel()) {
                            listener.onErroConexao(erro);
                        }
                    }
                }
        );

        Log.d(
                TAG,
                "Comunicação com o servidor registrada."
        );
    }

    // =====================================================
    // REMOVER COMUNICAÇÃO
    // =====================================================

    public void removerCallback() {

        gerenciadorSessao.removerCallback();

        Log.d(
                TAG,
                "Comunicação com o servidor encerrada."
        );
    }
}
