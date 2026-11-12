package com.example.rpghqconspiration;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

public class ServicoConexao {

    private static final String TAG = "ServicoConexao";

    private static final String URL_SERVIDOR =
            "wss://rpghqserver-1.onrender.com";

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
    private volatile String codigoPar;
    private volatile String papelUsuario = "";

    // Só fica true depois de receber control_granted do servidor.
    private volatile boolean controleConcedido = false;

    // Indica que solicitamos o controle e aguardamos a resposta.
    private volatile boolean solicitacaoPendente = false;

    private final AtomicBoolean entradaEnviada =
            new AtomicBoolean(false);

    public ServicoConexao(Callback callback) {
        this.callback = callback;
    }

    public void definirCallback(Callback callback) {
        this.callback = callback;
    }

    // =====================================================
    // CONEXÃO
    // =====================================================

    public synchronized void conectar(String codigo) {

        if (codigo == null || codigo.trim().isEmpty()) {
            avisarErro("Digite o código da sala.");
            return;
        }

        if (estaConectado()) {
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
        papelUsuario = "";

        conectado = false;
        controleConcedido = false;
        solicitacaoPendente = false;

        entradaEnviada.set(false);

        try {

            cliente = new WebSocketClient(new URI(URL_SERVIDOR)) {

                @Override
                public void onOpen(ServerHandshake handshake) {

                    conectado = true;

                    Log.d(TAG, "WebSocket conectado.");

                    enviarEntradaNaSala();
                    avisarConectado();
                }

                @Override
                public void onMessage(String mensagemRecebida) {

                    Log.d(
                            TAG,
                            "Mensagem recebida: " + mensagemRecebida
                    );

                    try {

                        JSONObject mensagem =
                                new JSONObject(mensagemRecebida);

                        processarEstadoDaConexao(mensagem);
                        avisarMensagem(mensagem);

                    } catch (JSONException e) {

                        Log.e(
                                TAG,
                                "Mensagem JSON inválida.",
                                e
                        );

                        avisarErro("Mensagem recebida inválida.");
                    }
                }

                @Override
                public void onClose(
                        int codigoFechamento,
                        String motivo,
                        boolean remoto) {

                    conectado = false;
                    controleConcedido = false;
                    solicitacaoPendente = false;

                    entradaEnviada.set(false);

                    Log.d(
                            TAG,
                            "WebSocket encerrado: " + motivo
                    );

                    avisarDesconectado(motivo);
                }

                @Override
                public void onError(Exception erro) {

                    conectado = false;
                    controleConcedido = false;
                    solicitacaoPendente = false;

                    Log.e(TAG, "Erro no WebSocket.", erro);

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
            controleConcedido = false;
            solicitacaoPendente = false;

            Log.e(
                    TAG,
                    "Não foi possível iniciar a conexão.",
                    e
            );

            avisarErro(
                    "Não foi possível iniciar a conexão: "
                            + e.getMessage()
            );
        }
    }

    // =====================================================
    // PROCESSAMENTO DAS MENSAGENS DO SERVIDOR
    // =====================================================

    private void processarEstadoDaConexao(JSONObject mensagem) {

        String tipo = mensagem.optString("type", "");

        switch (tipo) {

            case "joined":

                papelUsuario =
                        mensagem.optString("role", "");

                Log.d(
                        TAG,
                        "Papel atribuído pelo servidor: "
                                + papelUsuario
                );

                break;

            case "control_granted":

                controleConcedido = true;
                solicitacaoPendente = false;

                Log.d(
                        TAG,
                        "Controle autorizado pelo servidor."
                );

                break;

            case "control_busy":

                controleConcedido = false;
                solicitacaoPendente = false;

                Log.d(
                        TAG,
                        "Controle ocupado pelo outro usuário."
                );

                break;

            case "control_released":

                controleConcedido = false;
                solicitacaoPendente = false;

                Log.d(
                        TAG,
                        "Controle liberado pelo servidor."
                );

                break;

            case "peer_disconnected":

                controleConcedido = false;
                solicitacaoPendente = false;

                Log.d(
                        TAG,
                        "O outro usuário desconectou."
                );

                break;

            case "control":

                // Esta mensagem é enviada pelo servidor ao parceiro.
                // Quando active=false, o parceiro liberou o controle.
                if (!mensagem.optBoolean("active", false)) {

                    controleConcedido = false;
                    solicitacaoPendente = false;
                }

                break;

            case "error":

                solicitacaoPendente = false;

                Log.e(
                        TAG,
                        "Erro informado pelo servidor: "
                                + mensagem.optString("message", "")
                );

                break;
        }
    }

    // =====================================================
    // ENTRADA NA SALA
    // =====================================================

    private void enviarEntradaNaSala() {

        if (!conectado
                || codigoPar == null
                || !entradaEnviada.compareAndSet(false, true)) {

            return;
        }

        try {

            JSONObject mensagem = new JSONObject();

            mensagem.put("type", "join");
            mensagem.put("pair", codigoPar);

            enviarJson(mensagem);

            Log.d(
                    TAG,
                    "Entrada na sala enviada: " + codigoPar
            );

        } catch (JSONException e) {

            entradaEnviada.set(false);

            Log.e(
                    TAG,
                    "Erro ao preparar entrada na sala.",
                    e
            );

            avisarErro("Erro ao entrar na sala.");
        }
    }

    // =====================================================
    // ENVIAR SOLICITAÇÃO OU ATUALIZAÇÃO DE CONTROLE
    // =====================================================

    public void enviarControle(boolean ativo, float progresso) {

        if (!estaConectado()) {

            Log.w(
                    TAG,
                    "Controle não enviado: conexão indisponível."
            );

            return;
        }

        // Uma solicitação de controle começa quando ativo=true.
        // O servidor decide se concede ou não a autorização.
        if (ativo && !controleConcedido) {
            solicitacaoPendente = true;
        }

        // Ao liberar, o estado local também deixa de ser proprietário.
        if (!ativo) {
            controleConcedido = false;
            solicitacaoPendente = false;
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

            Log.d(
                    TAG,
                    "Controle enviado: " + mensagem
            );

        } catch (JSONException e) {

            Log.e(
                    TAG,
                    "Erro ao preparar comando de controle.",
                    e
            );

            avisarErro(
                    "Não foi possível preparar o comando."
            );
        }
    }

    // =====================================================
    // ENVIO JSON
    // =====================================================

    private synchronized void enviarJson(JSONObject mensagem) {

        WebSocketClient conexaoAtual = cliente;

        if (conexaoAtual == null
                || conexaoAtual.getReadyState()
                != ReadyState.OPEN) {

            Log.w(
                    TAG,
                    "Mensagem não enviada: WebSocket fechado."
            );

            return;
        }

        try {

            conexaoAtual.send(mensagem.toString());

            Log.d(
                    TAG,
                    "Mensagem enviada: " + mensagem
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Erro ao enviar mensagem.",
                    e
            );

            avisarErro(
                    "Falha ao enviar dados ao parceiro."
            );
        }
    }

    // =====================================================
    // CONSULTAS DE ESTADO
    // =====================================================

    public boolean estaConectado() {

        WebSocketClient conexaoAtual = cliente;

        return conectado
                && conexaoAtual != null
                && conexaoAtual.getReadyState()
                == ReadyState.OPEN;
    }

    public String getCodigoPar() {
        return codigoPar;
    }

    public String getPapelUsuario() {
        return papelUsuario;
    }

    public boolean possuiControle() {
        return controleConcedido;
    }

    public boolean possuiSolicitacaoPendente() {
        return solicitacaoPendente;
    }

    // =====================================================
    // DESCONECTAR
    // =====================================================

    public synchronized void desconectar() {

        conectado = false;
        controleConcedido = false;
        solicitacaoPendente = false;

        entradaEnviada.set(false);

        WebSocketClient conexaoAtual = cliente;
        cliente = null;

        if (conexaoAtual != null) {

            try {

                if (conexaoAtual.isOpen()) {
                    conexaoAtual.close();
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Erro ao fechar WebSocket.",
                        e
                );
            }
        }
    }

    // =====================================================
    // CALLBACKS
    // =====================================================

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