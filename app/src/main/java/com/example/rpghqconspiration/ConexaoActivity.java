package com.example.rpghqconspiration;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class ConexaoActivity extends AppCompatActivity {

    private static final String TAG = "ConexaoActivity";

    /*
     * Instância única compartilhada entre a tela de conexão
     * e a tela principal.
     */
    public static ServicoConexao conexao;

    private EditText campoCodigoPar;
    private Button botaoConectar;
    private TextView statusConexao;

    private boolean abriuTelaPrincipal = false;
    private boolean aguardandoParceiro = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexao);

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

                Log.d(TAG,
                        "Conexão encerrada: " + motivo);

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

    private void processarMensagem(JSONObject mensagem) {

        String tipo = mensagem.optString("type", "");

        Log.d(TAG,
                "Evento recebido do servidor: " + mensagem);

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
                        "Evento não tratado na tela de conexão: "
                                + tipo);

                break;
            }
        }
    }

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

        /*
         * A instância de ServicoConexao permanece ativa.
         * A MainActivity irá reutilizar esta mesma instância.
         */
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

    private void liberarTelaConexao() {

        botaoConectar.setEnabled(true);
        campoCodigoPar.setEnabled(true);

        aguardandoParceiro = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * Ao voltar da MainActivity, permite uma nova tentativa
         * sem destruir automaticamente a conexão compartilhada.
         */
        if (!abriuTelaPrincipal) {
            if (conexao != null) {
                conexao.definirCallback(criarCallback());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*
         * Não chamar conexao.desconectar() aqui.
         * A conexão é compartilhada com a MainActivity.
         */
    }
}