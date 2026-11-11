package com.example.rpghqconspiration;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

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
    private TextView statusConexao;

    private boolean abriuTelaPrincipal = false;
    private boolean aguardandoParceiro = false;

    // =====================================================
    // MODO IMERSIVO
    // =====================================================

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
        statusConexao = findViewById(R.id.statusConexao);

        botaoConectar.setOnClickListener(v -> iniciarConexao());

        /*
         * Não cria outra conexão se já existe uma instância.
         */

        if (conexao == null) {

            conexao = new ServicoConexao(criarCallback());

        } else {

            conexao.definirCallback(criarCallback());

            if (conexao.estaConectado()) {

                statusConexao.setText(
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

        abriuTelaPrincipal = false;
        aguardandoParceiro = false;

        botaoConectar.setEnabled(false);
        campoCodigoPar.setEnabled(false);

        statusConexao.setText(
                "Conectando ao servidor..."
        );

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

                statusConexao.setText(
                        "Servidor conectado. Entrando na sala..."
                );
            }

            @Override
            public void onDesconectado(String motivo) {

                Log.d(TAG, "Conexão encerrada: " + motivo);

                if (!abriuTelaPrincipal) {

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

                if (!abriuTelaPrincipal) {

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

        String tipo = mensagem.optString("type", "");

        Log.d(TAG, "Evento recebido do servidor: " + mensagem);

        switch (tipo) {

            case "joined": {

                int usuarios = mensagem.optInt("users", 0);

                if (usuarios >= 2) {

                    abrirTelaPrincipal();

                } else {

                    aguardandoParceiro = true;

                    statusConexao.setText(
                            "Conectado! Aguardando o segundo usuário..."
                    );
                }

                break;
            }

            case "peer_connected": {

                statusConexao.setText(
                        "Parceiro conectado! Abrindo aplicativo..."
                );

                abrirTelaPrincipal();

                break;
            }

            case "peer_disconnected": {

                statusConexao.setText(
                        "Seu parceiro desconectou."
                );

                break;
            }

            case "error": {

                String erro = mensagem.optString(
                        "message",
                        "O servidor informou um erro."
                );

                statusConexao.setText(erro);

                liberarTelaConexao();

                break;
            }

            default: {

                Log.d(TAG,
                        "Evento não tratado: " + tipo);

                break;
            }
        }
    }

    // =====================================================
    // ABRIR TELA PRINCIPAL
    // =====================================================

    private void abrirTelaPrincipal() {

        if (abriuTelaPrincipal) {
            return;
        }

        if (conexao == null || !conexao.estaConectado()) {

            statusConexao.setText(
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

        intent.putExtra(
                "codigo_par",
                conexao.getCodigoPar()
        );

        startActivity(intent);
    }

    // =====================================================
    // LIBERAR TELA
    // =====================================================

    private void liberarTelaConexao() {

        botaoConectar.setEnabled(true);
        campoCodigoPar.setEnabled(true);

        aguardandoParceiro = false;
    }

    // =====================================================
    // RETORNO À TELA
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        ativarModoImersivo();

        if (!abriuTelaPrincipal) {

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

        super.onDestroy();

        /*
         * Não desconectar o WebSocket aqui.
         * A conexão é compartilhada com MainActivity.
         */
    }
}