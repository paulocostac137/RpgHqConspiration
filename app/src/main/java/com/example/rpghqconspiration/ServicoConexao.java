package com.example.rpghqconspiration;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.enums.ReadyState;

public class ServicoConexao {

    private static final String TAG = "ServicoConexao";

    private static final String URL_SERVIDOR =
            "wss://rpghqserver.onrender.com";

    public interface Callback {

        void onConectado();

        void onDesconectado(String motivo);

        void onMensagem(JSONObject mensagem);

        void onErro(String erro);
    }

    private final Handler handlerPrincipal =
            new Handler(Looper.getMainLooper());

    private volatile Callback callback;

    private volatile WebSocketClient cliente;

    private volatile boolean conectado = false;

    private final AtomicBoolean entradaEnviada =
            new AtomicBoolean(false);

    private String codigoPar;

    public ServicoConexao(Callback callback) {
        this.callback = callback;
    }

    public void definirCallback(Callback callback) {
        this.callback = callback;
    }

    // =========================================
    // CONEXÃO
    // =========================================

    public synchronized void conectar(String codigo) {

        if (codigo == null || codigo.trim().isEmpty()) {
            avisarErro("Digite o código da sala.");
            return;
        }

        if (conectado) {
            Log.d(TAG, "Já existe uma conexão ativa.");
            avisarConectado();
            return;
        }

        if (cliente != null) {

            ReadyState estado = cliente.getReadyState();

            if (estado == ReadyState.OPEN
                    || estado == ReadyState.NOT_YET_CONNECTED) {

                Log.d(TAG, "Conexão já está em andamento.");
                return;
            }
        }

        codigoPar = codigo.trim();

        conectado = false;

        entradaEnviada.set(false);

        try {

            cliente = new WebSocketClient(
                    new URI(URL_SERVIDOR)
            ) {

                @Override
                public void onOpen(ServerHandshake handshake) {

                    conectado = true;

                    Log.d(TAG, "WebSocket conectado.");

                    enviarEntradaNaSala();

                    avisarConectado();
                }

                @Override
                public void onMessage(String mensagemRecebida) {

                    Log.d(TAG,
                            "Mensagem recebida: " + mensagemRecebida);

                    try {

                        JSONObject mensagem =
                                new JSONObject(mensagemRecebida);

                        avisarMensagem(mensagem);

                    } catch (JSONException e) {

                        Log.e(TAG,
                                "Mensagem JSON inválida.",
                                e);

                        avisarErro(
                                "Mensagem recebida inválida."
                        );
                    }
                }

                @Override
                public void onClose(
                        int codigo,
                        String motivo,
                        boolean remoto) {

                    conectado = false;

                    entradaEnviada.set(false);

                    Log.d(TAG,
                            "WebSocket encerrado: " + motivo);

                    avisarDesconectado(motivo);
                }

                @Override
                public void onError(Exception erro) {

                    conectado = false;

                    Log.e(TAG,
                            "Erro no WebSocket.",
                            erro);

                    String mensagem = erro.getMessage();

                    if (mensagem == null
                            || mensagem.trim().isEmpty()) {

                        mensagem =
                                "Erro na conexão com o servidor.";
                    }

                    avisarErro(mensagem);
                }
            };

            cliente.connect();

        } catch (Exception e) {

            conectado = false;

            Log.e(TAG,
                    "Não foi possível iniciar a conexão.",
                    e);

            avisarErro(
                    "Não foi possível iniciar a conexão: "
                            + e.getMessage()
            );
        }
    }

    // =========================================
    // ENTRADA NA SALA
    // =========================================

    private void enviarEntradaNaSala() {

        if (!conectado
                || codigoPar == null
                || !entradaEnviada.compareAndSet(false, true)) {

            return;
        }

        try {

            JSONObject mensagem = new JSONObject();

            mensagem.put("type", "join");

            // O servidor Python espera o campo "pair".
            mensagem.put("pair", codigoPar);

            enviarJson(mensagem);

            Log.d(TAG,
                    "Entrada na sala enviada: " + codigoPar);

        } catch (JSONException e) {

            entradaEnviada.set(false);

            Log.e(TAG,
                    "Erro ao preparar entrada na sala.",
                    e);

            avisarErro("Erro ao entrar na sala.");
        }
    }

    // =========================================
    // ENVIAR CONTROLES
    // =========================================

    public void enviarControle(
            boolean ativo,
            float progresso) {

        if (!estaConectado()) {

            Log.w(TAG,
                    "Controle não enviado: conexão indisponível.");

            return;
        }

        float progressoLimitado = Math.max(
                0.0f,
                Math.min(1.0f, progresso)
        );

        try {

            JSONObject mensagem = new JSONObject();

            mensagem.put("type", "control");

            mensagem.put("active", ativo);

            mensagem.put("progress", progressoLimitado);

            enviarJson(mensagem);

            Log.d(TAG,
                    "Controle enviado: " + mensagem);

        } catch (JSONException e) {

            Log.e(TAG,
                    "Erro ao preparar comando de controle.",
                    e);

            avisarErro(
                    "Não foi possível preparar o comando."
            );
        }
    }

    // =========================================
    // ENVIO JSON
    // =========================================

    private synchronized void enviarJson(JSONObject mensagem) {

        WebSocketClient conexaoAtual = cliente;

        if (conexaoAtual == null
                || conexaoAtual.getReadyState() != ReadyState.OPEN) {

            Log.w(TAG,
                    "Mensagem não enviada: WebSocket fechado.");

            return;
        }

        try {

            conexaoAtual.send(mensagem.toString());

            Log.d(TAG,
                    "Mensagem enviada: " + mensagem);

        } catch (Exception e) {

            Log.e(TAG,
                    "Erro ao enviar mensagem.",
                    e);

            avisarErro(
                    "Falha ao enviar dados ao parceiro."
            );
        }
    }

    // =========================================
    // VERIFICAR CONEXÃO
    // =========================================

    public boolean estaConectado() {

        WebSocketClient conexaoAtual = cliente;

        return conectado
                && conexaoAtual != null
                && conexaoAtual.getReadyState() == ReadyState.OPEN;
    }

    public String getCodigoPar() {
        return codigoPar;
    }

    // =========================================
    // DESCONECTAR
    // =========================================

    public synchronized void desconectar() {

        conectado = false;

        entradaEnviada.set(false);

        WebSocketClient conexaoAtual = cliente;

        cliente = null;

        if (conexaoAtual != null) {

            try {

                if (conexaoAtual.isOpen()) {
                    conexaoAtual.close();
                }

            } catch (Exception e) {

                Log.e(TAG,
                        "Erro ao fechar WebSocket.",
                        e);
            }
        }
    }

    // =========================================
    // CALLBACKS
    // =========================================

    private void avisarConectado() {

        handlerPrincipal.post(() -> {

            Callback atual = callback;

            if (atual != null) {
                atual.onConectado();
            }
        });
    }

    private void avisarDesconectado(String motivo) {

        handlerPrincipal.post(() -> {

            Callback atual = callback;

            if (atual != null) {
                atual.onDesconectado(motivo);
            }
        });
    }

    private void avisarMensagem(JSONObject mensagem) {

        handlerPrincipal.post(() -> {

            Callback atual = callback;

            if (atual != null) {
                atual.onMensagem(mensagem);
            }
        });
    }

    private void avisarErro(String erro) {

        handlerPrincipal.post(() -> {

            Callback atual = callback;

            if (atual != null) {
                atual.onErro(erro);
            }
        });
    }
}