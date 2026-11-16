package com.example.rpghqconspiration;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class ConexaoActivity extends AppCompatActivity {

    private static final String TAG = "ConexaoActivity";

    /*
     * Instância única compartilhada entre as telas.
     */
    public static ServicoConexao conexao;

    private EditText campoCodigoPar;
    private Button botaoConectar;
    private Button botaoUsarSemConexao;
    private View controlesConexao;
    private TextView statusConexao;
    private TextView statusInicial;
    private TextView tituloModo;

    private ConexaoFlow conexaoFlow;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int etapaAnimada;
    private int quantidadeReticencias;
    private int etapasConcluidas;
    private long inicioEtapaAtual;
    private boolean mensagemSalaPendenteAgendada;
    private JSONObject mensagemSalaPendente;
    private final Runnable concluirEtapaTres = () -> {
        if (etapaAnimada != 3 || isFinishing() || isDestroyed()) {
            return;
        }

        etapasConcluidas = 3;
        atualizarListaEtapas();
    };
    private final Runnable concluirEtapaUm = () -> {
        if (etapaAnimada != 1 || isFinishing() || isDestroyed()) {
            return;
        }

        etapasConcluidas = 1;
        iniciarEtapa(2);
        if (mensagemSalaPendente != null) {
            agendarConclusaoEtapaDois();
        }
    };
    private final Runnable concluirEtapaDois = () -> {
        if (etapaAnimada != 2 || mensagemSalaPendente == null
                || isFinishing() || isDestroyed()) {
            mensagemSalaPendenteAgendada = false;
            return;
        }

        etapasConcluidas = 2;
        atualizarListaEtapas();
        JSONObject mensagem = mensagemSalaPendente;
        mensagemSalaPendente = null;
        mensagemSalaPendenteAgendada = false;
        if (conexaoFlow != null) {
            conexaoFlow.processarMensagem(mensagem);
        }
    };
    private final Runnable animarStatus = new Runnable() {
        @Override
        public void run() {
            if (etapaAnimada == 0 || isFinishing() || isDestroyed()) {
                return;
            }

            quantidadeReticencias = (quantidadeReticencias + 1) % 4;
            atualizarListaEtapas();
            handler.postDelayed(this, 500);
        }
    };

    // =====================================================
    // MODO IMERSIVO
    // =====================================================

    private void ativarModoImersivo() {
        WindowUiController.ativarModoImersivo(getWindow());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            ativarModoImersivo();
        }
    }

    // =====================================================
    // ONCREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conexao);

        ativarModoImersivo();

        campoCodigoPar = findViewById(R.id.campoCodigoPar);
        botaoConectar = findViewById(R.id.botaoConectar);
        botaoUsarSemConexao = findViewById(R.id.botaoUsarSemConexao);
        controlesConexao = findViewById(R.id.controlesConexao);
        statusConexao = findViewById(R.id.statusConexao);
        statusInicial = findViewById(R.id.statusInicial);
        tituloModo = findViewById(R.id.tituloModo);

        conexaoFlow = new ConexaoFlow(new ConexaoFlow.Callback() {
            @Override
            public void onStatusUpdated(String mensagem) {
                mostrarStatusDuranteConexao();
                if (mensagem.startsWith("Etapa 3 de 3:")) {
                    iniciarEtapa(3);
                } else {
                    pararAnimacaoStatus();
                    statusConexao.setText(mensagem);
                }
            }

            @Override
            public void onLiberarTelaConexao() {
                pararAnimacaoStatus();
                controlesConexao.setVisibility(View.VISIBLE);
                statusInicial.setText(statusConexao.getText());
                statusConexao.setVisibility(View.GONE);
                statusInicial.setVisibility(View.VISIBLE);
                botaoConectar.setEnabled(true);
                campoCodigoPar.setEnabled(true);
            }

            @Override
            public void onAbrirTelaPrincipal() {
                abrirTelaPrincipal();
            }
        });

        botaoConectar.setOnClickListener(v -> iniciarConexao());
        botaoUsarSemConexao.setOnClickListener(v -> abrirModoIndividual());

        /*
         * Não cria outra conexão se já existe uma instância.
         */

        if (conexao == null) {

            conexao = new ServicoConexao(criarCallback());

        } else {

            conexao.definirCallback(criarCallback());

            if (conexao.estaConectado()) {

                statusInicial.setText(
                        "Conectado. Aguardando confirmação da sala..."
                );
            }
        }
    }

    // =====================================================
    // INICIAR CONEXÃO
    // =====================================================

    private void iniciarConexao() {

        if (conexao != null && conexao.estaConectado()) {

            tituloModo.setText("Iniciando modo online");
            statusInicial.setText(
                    "Você já está conectado ao servidor."
            );
            statusInicial.setVisibility(View.VISIBLE);
            statusConexao.setText(
                    "Você já está conectado ao servidor."
            );

            return;
        }

        String codigo = campoCodigoPar.getText()
                .toString()
                .trim();

        if (codigo.isEmpty()) {

            campoCodigoPar.setError("Digite o código da sala.");

            campoCodigoPar.requestFocus();

            return;
        }

        tituloModo.setText("Iniciando modo online");

        if (conexaoFlow != null) {
            conexaoFlow.resetar();
        }

        InputMethodManager teclado = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        if (teclado != null) {
            teclado.hideSoftInputFromWindow(
                    campoCodigoPar.getWindowToken(),
                    0
            );
        }
        campoCodigoPar.clearFocus();
        botaoConectar.setEnabled(false);
        campoCodigoPar.setEnabled(false);
        // Mantém o espaço reservado para os botões para evitar que o formulário se mova.
        controlesConexao.setVisibility(View.INVISIBLE);
        mostrarStatusDuranteConexao();

        iniciarProgresso();

        if (conexao == null) {

            conexao = new ServicoConexao(criarCallback());

        } else {

            conexao.definirCallback(criarCallback());
        }

        conexao.conectar(codigo);
    }

    // =====================================================
    // CALLBACK DA CONEXÃO
    // =====================================================

    private ServicoConexao.Callback criarCallback() {

        return new ServicoConexao.Callback() {

            @Override
            public void onConectado() {

                Log.d(TAG, "Conexão WebSocket estabelecida.");

                mostrarStatusDuranteConexao();
                handler.removeCallbacks(concluirEtapaUm);
                long tempoRestante = Math.max(
                        0L,
                        2000L - (SystemClock.uptimeMillis() - inicioEtapaAtual)
                );
                handler.postDelayed(concluirEtapaUm, tempoRestante);
            }

            @Override
            public void onDesconectado(String motivo) {

                Log.d(TAG, "Conexão encerrada: " + motivo);

                if (conexaoFlow == null || !conexaoFlow.isAbriuTelaPrincipal()) {

                    statusConexao.setText(
                            "Conexão encerrada. Tente novamente."
                    );

                    liberarTelaConexao();
                }
            }

            @Override
            public void onMensagem(JSONObject mensagem) {

                processarMensagem(mensagem);
            }

            @Override
            public void onErro(String erro) {

                Log.e(TAG, erro);

                if (conexaoFlow == null || !conexaoFlow.isAbriuTelaPrincipal()) {

                    statusConexao.setText(
                            "Erro: " + erro
                    );

                    liberarTelaConexao();
                }
            }
        };
    }

    // =====================================================
    // PROCESSAR MENSAGENS
    // =====================================================

    private void processarMensagem(JSONObject mensagem) {
        if (conexaoFlow != null) {
            String tipo = mensagem == null
                    ? ""
                    : mensagem.optString("type", "");
            if ("joined".equals(tipo) || "peer_connected".equals(tipo)) {
                mensagemSalaPendente = mensagem;
                if (etapaAnimada == 2) {
                    agendarConclusaoEtapaDois();
                }
                return;
            }
            conexaoFlow.processarMensagem(mensagem);
        }
    }

    // =====================================================
    // ABRIR TELA PRINCIPAL
    // =====================================================

    private void abrirTelaPrincipal() {

        if (conexaoFlow != null && conexaoFlow.isAbriuTelaPrincipal()) {
            return;
        }

        if (conexao == null || !conexao.estaConectado()) {

            statusConexao.setText(
                    "A conexão com o servidor foi perdida."
            );

            liberarTelaConexao();

            return;
        }

        pararAnimacaoStatus();

        if (conexaoFlow != null) {
            conexaoFlow.setAbriuTelaPrincipal(true);
        }

        Intent intent = new Intent(
                ConexaoActivity.this,
                MainActivity.class
        );

        intent.putExtra(
                "codigo_par",
                conexao.getCodigoPar()
        );

        startActivity(intent);
    }

    private void abrirModoIndividual() {
        pararAnimacaoStatus();
        Intent intent = new Intent(
                ConexaoActivity.this,
                MainActivity.class
        );
        intent.putExtra("modo_individual", true);
        startActivity(intent);
    }

    private void iniciarProgresso() {
        pararAnimacaoStatus();
        etapasConcluidas = 0;
        mensagemSalaPendente = null;
        mensagemSalaPendenteAgendada = false;
        iniciarEtapa(1);
    }

    private void iniciarEtapa(int etapa) {
        mostrarStatusDuranteConexao();
        etapaAnimada = etapa;
        inicioEtapaAtual = SystemClock.uptimeMillis();
        quantidadeReticencias = 0;
        handler.removeCallbacks(animarStatus);
        handler.removeCallbacks(concluirEtapaTres);
        animarStatus.run();
        if (etapa == 3) {
            handler.postDelayed(concluirEtapaTres, 1000L);
        }
    }

    private void agendarConclusaoEtapaDois() {
        if (mensagemSalaPendenteAgendada || etapaAnimada != 2) {
            return;
        }

        mensagemSalaPendenteAgendada = true;
        long tempoRestante = Math.max(
                0L,
                2000L - (SystemClock.uptimeMillis() - inicioEtapaAtual)
        );
        handler.postDelayed(concluirEtapaDois, tempoRestante);
    }

    private void atualizarListaEtapas() {
        SpannableStringBuilder texto = new SpannableStringBuilder();
        adicionarEtapa(
                texto,
                etapasConcluidas >= 1
                        ? "1. Conectado ao Servidor"
                        : "1. Conectando ao servidor",
                etapaAnimada == 1 ? reticencias() : "",
                etapasConcluidas >= 1 ? Color.GREEN : Color.DKGRAY,
                Color.WHITE
        );
        texto.append('\n');
        adicionarEtapa(
                texto,
                etapasConcluidas >= 2
                        ? "2. Dentro da Sala"
                        : "2. Entrando na sala",
                etapaAnimada == 2 ? reticencias() : "",
                etapasConcluidas >= 2 ? Color.GREEN : Color.DKGRAY,
                Color.WHITE
        );
        texto.append('\n');
        adicionarEtapa(
                texto,
                "3. Aguardando outro participante",
                etapaAnimada == 3 ? reticencias() : "",
                etapasConcluidas >= 3 ? Color.GREEN : Color.DKGRAY,
                etapasConcluidas >= 3 ? Color.GREEN : Color.DKGRAY
        );
        statusConexao.setText(texto);
    }

    private void adicionarEtapa(
            SpannableStringBuilder texto,
            String mensagem,
            String reticencias,
            int cor,
            int corReticencias
    ) {
        int inicio = texto.length();
        texto.append(mensagem);
        texto.setSpan(
                new ForegroundColorSpan(cor),
                inicio,
                texto.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        int inicioReticencias = texto.length();
        texto.append(reticencias);
        if (!reticencias.isEmpty()) {
            texto.setSpan(
                    new ForegroundColorSpan(corReticencias),
                    inicioReticencias,
                    texto.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private String reticencias() {
        StringBuilder pontos = new StringBuilder();
        for (int i = 0; i < quantidadeReticencias; i++) {
            pontos.append('.');
        }
        return pontos.toString();
    }

    private void mostrarStatusDuranteConexao() {
        statusInicial.setVisibility(View.GONE);
        statusConexao.setVisibility(View.VISIBLE);
    }

    private void pararAnimacaoStatus() {
        etapaAnimada = 0;
        handler.removeCallbacks(animarStatus);
        handler.removeCallbacks(concluirEtapaUm);
        handler.removeCallbacks(concluirEtapaDois);
        handler.removeCallbacks(concluirEtapaTres);
        mensagemSalaPendenteAgendada = false;
    }

    // =====================================================
    // LIBERAR TELA
    // =====================================================

    private void liberarTelaConexao() {
        if (conexaoFlow != null) {
            conexaoFlow.liberarTelaConexao();
        }
    }

    // =====================================================
    // RETORNO À TELA
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        ativarModoImersivo();

        if (conexaoFlow == null || !conexaoFlow.isAbriuTelaPrincipal()) {

            if (conexao != null) {
                conexao.definirCallback(criarCallback());
            }
        }
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    @Override
    protected void onDestroy() {

        pararAnimacaoStatus();
        super.onDestroy();

        /*
         * Não desconectar o WebSocket aqui.
         * A conexão é compartilhada com MainActivity.
         */
    }
}