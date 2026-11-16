package com.example.rpghqconspiration;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class ConexaoActivity extends AppCompatActivity {

    private static final String TAG = "ConexaoActivity";
    private static final long ATRASO_ETAPA_MS = 2000L;

    public static ServicoConexao conexao;

    private final Handler handlerPrincipal =
            new Handler(Looper.getMainLooper());

    private EditText campoCodigoPar;
    private Button botaoConectar;
    private Button botaoUsarSemConexao;
    private TextView textoOu;
    private TextView statusConexao;
    private TextView textoStatusConexaoAtivo;
    private ScrollView statusConexaoAtivo;
    private FrameLayout layoutConexao;

    private boolean abriuTelaPrincipal = false;
    private boolean aberturaAgendada = false;
    private boolean statusAbaixoDoCampo = false;

    private void ativarModoImersivo() {

        Window window = getWindow();

        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.R) {

            WindowInsetsController controller =
                    window.getInsetsController();

            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars() |
                                WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) ativarModoImersivo();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexao);
        ativarModoImersivo();

        campoCodigoPar = findViewById(R.id.campoCodigoPar);
        botaoConectar = findViewById(R.id.botaoConectar);
        botaoUsarSemConexao = findViewById(R.id.botaoUsarSemConexao);
        textoOu = findViewById(R.id.textoOu);
        statusConexao = findViewById(R.id.statusConexao);
        statusConexaoAtivo = findViewById(R.id.statusConexaoAtivo);
        textoStatusConexaoAtivo =
                findViewById(R.id.textoStatusConexaoAtivo);
        layoutConexao = findViewById(R.id.layoutConexao);

        layoutConexao.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (statusAbaixoDoCampo) {
                        posicionarStatusAbaixoDoCampo();
                    }
                }
        );

        botaoConectar.setOnClickListener(v -> iniciarConexao());
        botaoUsarSemConexao.setOnClickListener(v -> abrirModoIndividual());

        if (conexao == null) {
            conexao = new ServicoConexao(criarCallback());
        } else {
            conexao.definirCallback(criarCallback());

            if (conexao.estaConectado()) {
                adicionarMensagemConexao(
                        "Conectado. Aguardando confirmação da sala..."
                );
            }
        }
    }

    private void abrirModoIndividual() {

        if (abriuTelaPrincipal) return;

        abriuTelaPrincipal = true;

        botaoConectar.setEnabled(false);
        botaoUsarSemConexao.setEnabled(false);
        campoCodigoPar.setEnabled(false);

        statusConexao.setText("Abrindo modo individual...");

        Intent intent = new Intent(
                ConexaoActivity.this,
                MainActivity.class
        );

        intent.putExtra("modo_individual", true);
        startActivity(intent);
    }

    private void iniciarConexao() {

        if (conexao != null && conexao.estaConectado()) {
            statusAbaixoDoCampo = true;
            mostrarSomenteStatusConexao();
            statusConexaoAtivo.setVisibility(View.VISIBLE);
            adicionarMensagemConexao(
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

        abriuTelaPrincipal = false;
        aberturaAgendada = false;

        botaoConectar.setEnabled(false);
        botaoUsarSemConexao.setEnabled(false);
        campoCodigoPar.setEnabled(false);
        mostrarSomenteStatusConexao();

        statusAbaixoDoCampo = true;
        statusConexao.setVisibility(View.INVISIBLE);
        statusConexaoAtivo.setVisibility(View.VISIBLE);
        adicionarMensagemConexao("Conectando ao servidor...");
        posicionarStatusAbaixoDoCampo();

        if (conexao == null) {
            conexao = new ServicoConexao(criarCallback());
        } else {
            conexao.definirCallback(criarCallback());
        }

        handlerPrincipal.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() &&
                    !abriuTelaPrincipal && statusAbaixoDoCampo) {
                conexao.conectar(codigo);
            }
        }, ATRASO_ETAPA_MS);
    }

    private ServicoConexao.Callback criarCallback() {

        return new ServicoConexao.Callback() {

            @Override
            public void onConectado() {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.d(TAG, "Conexão WebSocket estabelecida.");

                    adicionarMensagemConexao(
                            "Servidor conectado. Entrando na sala..."
                    );
                });
            }

            @Override
            public void onDesconectado(String motivo) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!abriuTelaPrincipal) {
                        adicionarMensagemConexao(
                                "Conexão encerrada. Tente novamente."
                        );
                        liberarTelaConexao();
                    }
                });
            }

            @Override
            public void onMensagem(JSONObject mensagem) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    processarMensagem(mensagem);
                });
            }

            @Override
            public void onErro(String erro) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    Log.e(TAG, erro);

                    if (!abriuTelaPrincipal) {
                        adicionarMensagemConexao("Erro: " + erro);
                        liberarTelaConexao();
                    }
                });
            }
        };
    }

    private void processarMensagem(JSONObject mensagem) {

        String tipo = mensagem.optString("type", "");

        Log.d(TAG, "Evento recebido do servidor: " + mensagem);

        switch (tipo) {

            case "joined": {
                int usuarios = mensagem.optInt("users", 0);

                if (usuarios >= 2) {
                    agendarAberturaTelaPrincipal(
                            "Parceiro encontrado! Abrindo aplicativo..."
                    );
                } else {
                    adicionarMensagemConexao(
                            "Conectado! Aguardando o segundo usuário..."
                    );
                }
                break;
            }

            case "peer_connected":
                agendarAberturaTelaPrincipal(
                        "Parceiro conectado! Abrindo aplicativo..."
                );
                break;

            case "peer_disconnected":
                adicionarMensagemConexao("Seu parceiro desconectou.");
                break;

            case "error": {
                String erro = mensagem.optString(
                        "message",
                        "O servidor informou um erro."
                );
                adicionarMensagemConexao(erro);
                liberarTelaConexao();
                break;
            }

            default:
                Log.d(TAG, "Evento não tratado: " + tipo);
                break;
        }
    }

    private void abrirTelaPrincipal() {

        if (abriuTelaPrincipal) return;

        if (conexao == null || !conexao.estaConectado()) {
            adicionarMensagemConexao(
                    "A conexão com o servidor foi perdida."
            );
            liberarTelaConexao();
            return;
        }

        abriuTelaPrincipal = true;

        Intent intent = new Intent(
                ConexaoActivity.this,
                MainActivity.class
        );

        intent.putExtra("codigo_par", conexao.getCodigoPar());
        intent.putExtra("papel_usuario", conexao.getPapelUsuario());
        startActivity(intent);
    }

    private void agendarAberturaTelaPrincipal(String mensagem) {

        adicionarMensagemConexao(mensagem);

        if (aberturaAgendada) {
            return;
        }

        aberturaAgendada = true;
        handlerPrincipal.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                abrirTelaPrincipal();
            }
        }, ATRASO_ETAPA_MS);
    }

    private void liberarTelaConexao() {

        botaoConectar.setEnabled(true);
        botaoUsarSemConexao.setEnabled(true);
        campoCodigoPar.setEnabled(true);
        botaoConectar.setVisibility(View.VISIBLE);
        textoOu.setVisibility(View.VISIBLE);
        botaoUsarSemConexao.setVisibility(View.VISIBLE);
    }

    private void mostrarSomenteStatusConexao() {

        botaoConectar.setVisibility(View.INVISIBLE);
        textoOu.setVisibility(View.INVISIBLE);
        botaoUsarSemConexao.setVisibility(View.INVISIBLE);
    }

    private void posicionarStatusAbaixoDoCampo() {

        statusConexaoAtivo.post(() -> {
            if (isFinishing() || isDestroyed() ||
                    !statusAbaixoDoCampo) {
                return;
            }

            int[] posicaoCampo = new int[2];
            int[] posicaoLayout = new int[2];
            campoCodigoPar.getLocationInWindow(posicaoCampo);
            layoutConexao.getLocationInWindow(posicaoLayout);

            float margem = 12f *
                    getResources().getDisplayMetrics().density;
            FrameLayout.LayoutParams parametros =
                    (FrameLayout.LayoutParams)
                            statusConexaoAtivo.getLayoutParams();
            parametros.topMargin = (int) (
                    posicaoCampo[1] - posicaoLayout[1] +
                            campoCodigoPar.getHeight() + margem
            );
            statusConexaoAtivo.setLayoutParams(parametros);
            statusConexaoAtivo.bringToFront();
        });
    }

    private void adicionarMensagemConexao(String mensagem) {

        if (textoStatusConexaoAtivo.length() > 0) {
            textoStatusConexaoAtivo.append("\n");
        }

        textoStatusConexaoAtivo.append(mensagem);
        statusConexaoAtivo.setVisibility(View.VISIBLE);
        statusConexaoAtivo.post(
                () -> statusConexaoAtivo.fullScroll(View.FOCUS_DOWN)
        );
        posicionarStatusAbaixoDoCampo();
    }

    @Override
    protected void onResume() {

        super.onResume();
        ativarModoImersivo();

        if (!abriuTelaPrincipal && conexao != null) {
            conexao.definirCallback(criarCallback());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // A conexão permanece compartilhada com MainActivity.
    }
}