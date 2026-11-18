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
    private static final long ATRASO_ENTRADA_SALA_MS = 2000L;

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
    private volatile int usuariosNaSala = 0;
    private volatile String codigoPar;
    private volatile String papelUsuario = "";
    private volatile String papelControleAtual = "A";

    private volatile boolean controleConcedido = false;
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
        papelControleAtual = "A";

        conectado = false;
        usuariosNaSala = 0;
        controleConcedido = false;
        solicitacaoPendente = false;

        entradaEnviada.set(false);

        try {

            cliente = new WebSocketClient(new URI(URL_SERVIDOR)) {

                @Override
                public void onOpen(ServerHandshake handshake) {

                    conectado = true;

                    Log.d(TAG, "WebSocket conectado.");

                    avisarConectado();
                    handlerPrincipal.postDelayed(
                            ServicoConexao.this::enviarEntradaNaSala,
                            ATRASO_ENTRADA_SALA_MS
                    );
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

            Log.d(TAG, "Iniciando conexão WebSocket com o servidor.");
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

                usuariosNaSala = mensagem.optInt("users", 0);
                papelUsuario =
                        mensagem.optString("role", "");
                papelControleAtual =
                        mensagem.optString("turn", "A");

                Log.d(
                        TAG,
                        "Papel atribuído pelo servidor: "
                                + papelUsuario
                );

                break;

            case "control":
                String turno = mensagem.optString("turn", "");
                if ("A".equals(turno) || "B".equals(turno)) {
                    papelControleAtual = turno;
                }
                if (!mensagem.optBoolean("active", false)) {

                    controleConcedido = false;
                    solicitacaoPendente = false;
                }

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

                usuariosNaSala = 1;
                controleConcedido = false;
                solicitacaoPendente = false;

                Log.d(
                        TAG,
                        "O outro usuário desconectou."
                );

                break;

            case "peer_connected":
                usuariosNaSala = 2;
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
    // ENVIAR CONTROLE
    // =====================================================

    public void enviarControle(boolean ativo, float progresso) {

        if (!estaConectado()) {

            Log.w(
                    TAG,
                    "Controle não enviado: conexão indisponível."
            );

            return;
        }

        if (ativo && !controleConcedido) {
            solicitacaoPendente = true;
        }

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

    public boolean enviarTrocaControle(String papel) {

        if (!"A".equals(papel) && !"B".equals(papel)) {
            avisarErro("Papel de controle inválido.");
            return false;
        }

        if (!estaConectado()) {
            avisarErro("Não foi possível trocar o controle: conexão indisponível.");
            return false;
        }

        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "control");
            mensagem.put("active", false);
            mensagem.put("progress", 0.0f);
            mensagem.put("turn", papel);
            if (!enviarJson(mensagem)) {
                return false;
            }
            papelControleAtual = papel;
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao preparar troca de controle.", e);
            avisarErro("Não foi possível preparar a troca de controle.");
            return false;
        }
    }

    public void enviarEstadoSolicitacaoTroca(boolean pendente) {

        if (!estaConectado()) {
            return;
        }

        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "control");
            mensagem.put("active", false);
            mensagem.put("progress", 0.0f);
            mensagem.put("turn_pending", pendente);
            enviarJson(mensagem);
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao preparar solicitação de troca.", e);
            avisarErro("Não foi possível preparar a solicitação de troca.");
        }
    }

    // =====================================================
    // ENVIO JSON
    // =====================================================

    private synchronized boolean enviarJson(JSONObject mensagem) {

        WebSocketClient conexaoAtual = cliente;

        if (conexaoAtual == null
                || conexaoAtual.getReadyState()
                != ReadyState.OPEN) {

            Log.w(
                    TAG,
                    "Mensagem não enviada: WebSocket fechado."
            );

            return false;
        }

        try {

            conexaoAtual.send(mensagem.toString());

            Log.d(
                    TAG,
                    "Mensagem enviada: " + mensagem
            );
            return true;

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Erro ao enviar mensagem.",
                    e
            );

            avisarErro(
                    "Falha ao enviar dados ao parceiro."
            );
            return false;
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

    public String getPapelControleAtual() {
        return papelControleAtual;
    }

    public boolean possuiControle() {
        return controleConcedido;
    }

    public boolean possuiSolicitacaoPendente() {
        return solicitacaoPendente;
    }

    public boolean estaSalaCompleta() {
        return usuariosNaSala >= 2;
    }

    public boolean enviarMensagemSincronizacao(JSONObject mensagem) {
        if (mensagem == null
                || !"sync".equals(mensagem.optString("type"))) {
            avisarErro("Mensagem de sincronização inválida.");
            return false;
        }

        if (!estaConectado()) {
            return false;
        }

        return enviarJson(mensagem);
    }

    // =====================================================
    // DESCONECTAR
    // =====================================================

    public synchronized void desconectar() {

        conectado = false;
        usuariosNaSala = 0;
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