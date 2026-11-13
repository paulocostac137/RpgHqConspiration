// Commit e Push Teste

package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;

import android.util.Log;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RPGHQ_SYNC";

    // =====================================================
    // PLANILHA
    // =====================================================

    private static final String PLANILHA_ID =
            "1pJ7XtHx0DLSCjsGj0zlJycm-nzqa7a8flnLfoG7k2ks";

    private static final String GID = "0";

    // =====================================================
    // TEMPOS
    // =====================================================

    private static final long JANELA_TEXTO_MS = 7000;
    private static final long TEMPO_TEXTO_MS = 5000;

    private static final long INTERVALO_ATUALIZACAO_MS = 50;
    private static final long INTERVALO_ANIMACAO_MS = 30;

    // =====================================================
    // CONFIGURAÇÃO DA MOVIMENTAÇÃO
    // =====================================================

    private static final int CICLOS_NECESSARIOS = 3;

    private static final float LIMITE_EXTREMO_SUPERIOR = 0.15f;
    private static final float LIMITE_EXTREMO_INFERIOR = 0.85f;

    private int ciclosConcluidos = 0;

    private int extremoInicial = -1;

    private boolean alcancouExtremoOposto = false;

    // =====================================================
    // COMPONENTES
    // =====================================================

    private ImageView imagemGif;

    private TextView status;
    private TextView textoPlanilha;

    private Button botaoProximo;

    private TextView controleEsquerdo;
    private TextView controleDireito;

    private FrameLayout areaGif;

    // =====================================================
    // DADOS DA PLANILHA
    // =====================================================

    private final ArrayList<String> nomes = new ArrayList<>();
    private final ArrayList<String> links = new ArrayList<>();
    private final ArrayList<String> textosPlanilha = new ArrayList<>();

    // =====================================================
    // EXECUÇÃO
    // =====================================================

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private ServicoConexao servicoConexao;

    private boolean atividadeEncerrada = false;

    private boolean modoIndividual = false;

    private boolean controleRemotoAtivo = false;

    private boolean carregando = false;

    private File pastaGifs;

    private int indiceAtual = -1;

    // Identifica a solicitação de GIF mais recente.
    // Impede que uma decodificação antiga substitua
    // uma animação solicitada posteriormente.

    private long geracaoExibicao = 0;

    // =====================================================
    // ANIMAÇÃO PRINCIPAL
    // =====================================================

    private Movie movie;

    private Bitmap quadroBitmap;
    private Canvas quadroCanvas;

    private final Paint pincel = new Paint(
            Paint.ANTI_ALIAS_FLAG |
                    Paint.FILTER_BITMAP_FLAG
    );

    private long posicaoMs = 0;
    private long inicioReproducao = 0;
    private long inicioExibicaoGif = 0;

    private boolean reproduzindo = false;

    // =====================================================
    // CONTROLES MULTITOQUE
    // =====================================================

    private boolean esquerdoPressionado = false;
    private boolean direitoPressionado = false;

    private boolean controleManualAtivo = false;

    private float posicaoDedoEsquerdo = 0;
    private float posicaoDedoDireito = 0;

    private int idDedoEsquerdo = -1;
    private int idDedoDireito = -1;

    // =====================================================
    // VERIFICAÇÃO DO CICLO DE VIDA
    // =====================================================

    private boolean atividadeDisponivel() {

        return !atividadeEncerrada &&
                !isFinishing() &&
                !isDestroyed();
    }

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

        if (hasFocus && atividadeDisponivel()) {
            ativarModoImersivo();
        }
    }

    // =====================================================
    // CONEXÃO COMPARTILHADA
    // =====================================================

    private void conectarServidor() {

        servicoConexao = ConexaoActivity.conexao;

        if (servicoConexao == null) {

            status.setText(
                    "ERRO: conexão com o servidor não encontrada."
            );

            Log.e(TAG,
                    "ServicoConexao não foi inicializado.");

            return;
        }

        if (!servicoConexao.estaConectado()) {

            status.setText(
                    "ERRO: conexão com o servidor foi perdida."
            );

            Log.e(TAG,
                    "A conexão compartilhada não está ativa.");

            return;
        }

        servicoConexao.definirCallback(
                new ServicoConexao.Callback() {

                    @Override
                    public void onConectado() {

                        handler.post(() -> {

                            if (!atividadeDisponivel()) return;

                            Log.d(TAG,
                                    "Conexão compartilhada ativa.");

                            status.setText(
                                    "Conectado ao servidor."
                            );
                        });
                    }

                    @Override
                    public void onDesconectado(String motivo) {

                        handler.post(() -> {

                            if (!atividadeDisponivel()) return;

                            Log.w(TAG,
                                    "Servidor desconectado: " + motivo);

                            controleRemotoAtivo = false;

                            finalizarMovimentoSeLiberado();

                            status.setText(
                                    "Servidor desconectado."
                            );
                        });
                    }

                    @Override
                    public void onMensagem(JSONObject mensagem) {

                        handler.post(() -> {

                            if (!atividadeDisponivel()) return;

                            processarMensagemServidor(mensagem);
                        });
                    }

                    @Override
                    public void onErro(String erro) {

                        handler.post(() -> {

                            if (!atividadeDisponivel()) return;

                            Log.e(TAG,
                                    "Erro de conexão: " + erro);

                            status.setText(
                                    "Erro de conexão: " + erro
                            );
                        });
                    }
                }
        );

        Log.d(TAG,
                "MainActivity reutilizando a conexão existente.");

        status.setText(
                "Conectado. Aguardando comandos..."
        );
    }

    // =====================================================
    // MENSAGENS RECEBIDAS
    // =====================================================

    private void processarMensagemServidor(JSONObject dados) {

        if (!atividadeDisponivel()) return;

        String tipo = dados.optString("type");

        Log.d(TAG,
                "Processando mensagem: " + dados);

        switch (tipo) {

            case "joined": {

                int usuarios = dados.optInt("users", 0);

                status.setText(
                        usuarios == 1
                                ? "Conectado. Aguardando parceiro..."
                                : "Conectado! Dois usuários na sala."
                );

                break;
            }

            case "peer_connected": {

                status.setText(
                        "PARCEIRO CONECTADO!"
                );

                Log.d(TAG,
                        "O parceiro entrou na sala.");

                break;
            }

            case "peer_disconnected": {

                controleRemotoAtivo = false;

                finalizarMovimentoSeLiberado();

                status.setText(
                        "Parceiro desconectado."
                );

                break;
            }

            case "error": {

                status.setText(
                        "Erro do servidor: " +
                                dados.optString("message")
                );

                break;
            }

            case "control": {

                processarControleRemoto(dados);

                break;
            }

            default: {

                Log.d(TAG,
                        "Mensagem não reconhecida: " + tipo);

                break;
            }
        }
    }

    // =====================================================
    // CONTROLE RECEBIDO DO PARCEIRO
    // =====================================================

    private void processarControleRemoto(JSONObject dados) {

        boolean ativo = dados.optBoolean("active", false);

        if (!ativo) {

            controleRemotoAtivo = false;

            Log.d(TAG,
                    "Parceiro liberou os controles.");

            finalizarMovimentoSeLiberado();

            return;
        }

        if (movie == null) {
            return;
        }

        float progressoRecebido =
                (float) dados.optDouble("progress", 0);

        if (Float.isNaN(progressoRecebido) ||
                Float.isInfinite(progressoRecebido)) {

            return;
        }

        progressoRecebido = Math.max(
                0f,
                Math.min(1f, progressoRecebido)
        );

        controleRemotoAtivo = true;

        pausarAnimacao();

        registrarProgressoMovimento(progressoRecebido);

        int duracao = obterDuracaoGif();

        posicaoMs = (long) (
                progressoRecebido * duracao
        );

        final float progressoFinal = progressoRecebido;

        areaGif.post(() -> {

            if (!atividadeDisponivel() || movie == null) {
                return;
            }

            int altura = areaGif.getHeight();

            int alturaBotao =
                    controleEsquerdo.getHeight();

            if (altura <= 0 || alturaBotao <= 0) {
                return;
            }

            float metadeBotao = alturaBotao / 2f;

            int centroY = (int) (
                    metadeBotao +
                            progressoFinal *
                                    (altura - 2f * metadeBotao)
            );

            moverBotao(
                    controleEsquerdo,
                    centroY,
                    true
            );

            moverBotao(
                    controleDireito,
                    centroY,
                    false
            );

            desenharQuadro();
        });
    }

    // =====================================================
    // ENVIO DE COMANDOS AO PARCEIRO
    // =====================================================

    private void enviarComandoControle(
            boolean ativo,
            float progresso) {

        if (modoIndividual) return;

        if (servicoConexao == null) {

            Log.w(TAG,
                    "Comando não enviado: serviço inexistente.");

            return;
        }

        if (!servicoConexao.estaConectado()) {

            Log.w(TAG,
                    "Comando não enviado: conexão indisponível.");

            return;
        }

        servicoConexao.enviarControle(
                ativo,
                progresso
        );

        Log.d(TAG,
                "Controle enviado. Ativo: " +
                        ativo +
                        " Progresso: " +
                        progresso);
    }

    // =====================================================
    // DURAÇÃO DO GIF
    // =====================================================

    private int obterDuracaoGif() {

        if (movie == null) {
            return 1000;
        }

        int duracao = movie.duration();

        return duracao > 0 ? duracao : 1000;
    }

    // =====================================================
    // CONTAGEM DOS CICLOS DE MOVIMENTAÇÃO
    // =====================================================

    private void registrarProgressoMovimento(float progresso) {

        if (!controleManualAtivo &&
                !controleRemotoAtivo) {

            return;
        }

        int extremoAtual = -1;

        if (progresso <= LIMITE_EXTREMO_SUPERIOR) {

            extremoAtual = 0;

        } else if (progresso >= LIMITE_EXTREMO_INFERIOR) {

            extremoAtual = 1;
        }

        if (extremoAtual == -1) {
            return;
        }

        if (extremoInicial == -1) {

            extremoInicial = extremoAtual;

            return;
        }

        if (!alcancouExtremoOposto &&
                extremoAtual != extremoInicial) {

            alcancouExtremoOposto = true;

            return;
        }

        if (alcancouExtremoOposto &&
                extremoAtual == extremoInicial) {

            ciclosConcluidos++;

            alcancouExtremoOposto = false;

            Log.d(
                    TAG,
                    "Ciclo completo: " +
                            ciclosConcluidos +
                            "/" +
                            CICLOS_NECESSARIOS
            );
        }
    }

    private void reiniciarContagemMovimentos() {

        ciclosConcluidos = 0;

        extremoInicial = -1;

        alcancouExtremoOposto = false;
    }

    // =====================================================
    // DECISÃO APÓS SOLTAR TODOS OS CONTROLES
    // =====================================================

    private void finalizarMovimentoSeLiberado() {

        if (esquerdoPressionado ||
                direitoPressionado ||
                controleManualAtivo ||
                controleRemotoAtivo) {

            return;
        }

        if (movie == null) {

            reiniciarContagemMovimentos();

            return;
        }

        if (ciclosConcluidos >= CICLOS_NECESSARIOS) {

            Log.d(
                    TAG,
                    "Três ciclos concluídos. Avançando para o próximo GIF."
            );

            reiniciarContagemMovimentos();

            proximoGif();

            return;
        }

        Log.d(
                TAG,
                "Movimento insuficiente: " +
                        ciclosConcluidos +
                        "/" +
                        CICLOS_NECESSARIOS +
                        ". Mantendo o GIF atual."
        );

        reiniciarContagemMovimentos();

        iniciarAnimacao();
    }

    // =====================================================
    // ATUALIZAÇÃO AUTOMÁTICA DA ANIMAÇÃO
    // =====================================================

    private final Runnable atualizadorGif = new Runnable() {

        @Override
        public void run() {

            if (!atividadeDisponivel() ||
                    movie == null ||
                    !reproduzindo) {

                return;
            }

            int duracao = obterDuracaoGif();

            long agora = SystemClock.uptimeMillis();

            long decorrido = agora - inicioReproducao;

            posicaoMs = (
                    posicaoMs + decorrido
            ) % duracao;

            inicioReproducao = agora;

            desenharQuadro();

            handler.postDelayed(
                    this,
                    INTERVALO_ANIMACAO_MS
            );
        }
    };

    // =====================================================
    // TEXTO DA PLANILHA
    // =====================================================

    private void configurarTextoPlanilha() {

        textoPlanilha.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(250, 0, 0, 0),
                                Color.argb(220, 0, 0, 0),
                                Color.argb(140, 0, 0, 0),
                                Color.argb(0, 0, 0, 0)
                        }
                )
        );

        textoPlanilha.setVisibility(View.GONE);

        areaGif.post(() -> {

            if (!atividadeDisponivel()) return;

            int altura = areaGif.getHeight();

            if (altura <= 0) return;

            FrameLayout.LayoutParams parametros =
                    (FrameLayout.LayoutParams)
                            textoPlanilha.getLayoutParams();

            parametros.width =
                    FrameLayout.LayoutParams.MATCH_PARENT;

            parametros.height = altura / 4;

            parametros.gravity = Gravity.TOP;

            textoPlanilha.setLayoutParams(parametros);
        });
    }

    private final Runnable atualizarTextoPlanilha =
            new Runnable() {

                @Override
                public void run() {

                    if (!atividadeDisponivel()) {
                        return;
                    }

                    if (movie == null || inicioExibicaoGif == 0) {

                        textoPlanilha.setVisibility(View.GONE);

                        return;
                    }

                    long decorrido =
                            SystemClock.uptimeMillis() -
                                    inicioExibicaoGif;

                    if (decorrido >= TEMPO_TEXTO_MS &&
                            decorrido < JANELA_TEXTO_MS) {

                        textoPlanilha.setVisibility(View.VISIBLE);

                    } else {

                        textoPlanilha.setVisibility(View.GONE);
                    }

                    if (decorrido < JANELA_TEXTO_MS) {

                        handler.postDelayed(
                                this,
                                INTERVALO_ATUALIZACAO_MS
                        );
                    }
                }
            };

    // =====================================================
    // ONCREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ativarModoImersivo();

        imagemGif = findViewById(R.id.imagemGif);

        status = findViewById(R.id.status);

        botaoProximo = findViewById(R.id.botaoProximo);

        controleEsquerdo =
                findViewById(R.id.controleEsquerdo);

        controleDireito =
                findViewById(R.id.controleDireito);

        areaGif = findViewById(R.id.areaGif);

        textoPlanilha =
                findViewById(R.id.textoPlanilha);

        configurarTextoPlanilha();

        pastaGifs = new File(
                getFilesDir(),
                "gifs"
        );

        if (!pastaGifs.exists() &&
                !pastaGifs.mkdirs()) {

            Log.e(TAG,
                    "Não foi possível criar a pasta de GIFs.");
        }

        botaoProximo.setOnClickListener(v -> {

            reiniciarContagemMovimentos();

            proximoGif();
        });

        configurarControles();

        modoIndividual = getIntent().getBooleanExtra(
                "modo_individual",
                false
        );

        if (modoIndividual) {

            servicoConexao = null;

            status.setText(
                    "Modo individual. Sem conexão com o servidor."
            );

            Log.d(
                    TAG,
                    "Aplicativo iniciado em modo individual."
            );

        } else {

            conectarServidor();
        }

        carregarPlanilha();
    }

    // =====================================================
    // CONFIGURAÇÃO DOS CONTROLES
    // =====================================================

    private void configurarControles() {

        areaGif.setMotionEventSplittingEnabled(false);

        View.OnTouchListener listenerMultitoque =
                (view, evento) -> {

                    processarEventoMultitoque(
                            view,
                            evento
                    );

                    return true;
                };

        controleEsquerdo.setOnTouchListener(
                listenerMultitoque
        );

        controleDireito.setOnTouchListener(
                listenerMultitoque
        );
    }

    // =====================================================
    // PROCESSAMENTO MULTITOQUE
    // =====================================================

    private void processarEventoMultitoque(
            View view,
            MotionEvent evento) {

        int acao = evento.getActionMasked();

        int indice = evento.getActionIndex();

        switch (acao) {

            case MotionEvent.ACTION_DOWN:

                atribuirPonteiro(
                        view,
                        evento,
                        indice
                );

                verificarControleManual();

                atualizarControleManual();

                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                atribuirPonteiro(
                        view,
                        evento,
                        indice
                );

                verificarControleManual();

                atualizarControleManual();

                break;

            case MotionEvent.ACTION_MOVE:

                atualizarPosicoesDedos(evento);

                atualizarControleManual();

                break;

            case MotionEvent.ACTION_POINTER_UP:

                atualizarPosicoesDedos(evento);

                int id =
                        evento.getPointerId(indice);

                liberarPonteiro(id);

                verificarLiberacao();

                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                liberarTodosOsDedos();

                verificarLiberacao();

                break;
        }
    }

    // =====================================================
    // IDENTIFICAÇÃO DOS DEDOS
    // =====================================================

    private void limparIdentificacaoDedos() {

        idDedoEsquerdo = -1;
        idDedoDireito = -1;

        esquerdoPressionado = false;
        direitoPressionado = false;

        controleManualAtivo = false;
    }

    // =====================================================
    // CONVERTE COORDENADAS PARA A TELA
    // =====================================================

    private float obterXNaTela(
            MotionEvent evento,
            int indice) {

        float deslocamentoX =
                evento.getRawX() - evento.getX(0);

        return evento.getX(indice) + deslocamentoX;
    }

    private float obterYNaTela(
            MotionEvent evento,
            int indice) {

        float deslocamentoY =
                evento.getRawY() - evento.getY(0);

        return evento.getY(indice) + deslocamentoY;
    }

    // =====================================================
    // IDENTIFICA QUAL CONTROLE RECEBEU O DEDO
    // =====================================================

    private int identificarControle(
            float x,
            float y) {

        int[] localizacao = new int[2];

        controleEsquerdo.getLocationOnScreen(
                localizacao
        );

        if (x >= localizacao[0] &&
                x <= localizacao[0] +
                        controleEsquerdo.getWidth() &&
                y >= localizacao[1] &&
                y <= localizacao[1] +
                        controleEsquerdo.getHeight()) {

            return 1;
        }

        controleDireito.getLocationOnScreen(
                localizacao
        );

        if (x >= localizacao[0] &&
                x <= localizacao[0] +
                        controleDireito.getWidth() &&
                y >= localizacao[1] &&
                y <= localizacao[1] +
                        controleDireito.getHeight()) {

            return 2;
        }

        return 0;
    }

    // =====================================================
    // ATRIBUI UM DEDO A UM CONTROLE
    // =====================================================

    private void atribuirPonteiro(
            View view,
            MotionEvent evento,
            int indice) {

        int id = evento.getPointerId(indice);

        float x = obterXNaTela(evento, indice);

        float y = obterYNaTela(evento, indice);

        int controle = identificarControle(x, y);

        if (controle == 0) {

            if (view == controleEsquerdo) {
                controle = 1;

            } else if (view == controleDireito) {
                controle = 2;
            }
        }

        if (controle == 1 && idDedoEsquerdo == -1) {

            idDedoEsquerdo = id;

            esquerdoPressionado = true;

            posicaoDedoEsquerdo = y;

            Log.d(TAG,
                    "Dedo no controle esquerdo. ID: " + id);

        } else if (controle == 2 &&
                idDedoDireito == -1) {

            idDedoDireito = id;

            direitoPressionado = true;

            posicaoDedoDireito = y;

            Log.d(TAG,
                    "Dedo no controle direito. ID: " + id);
        }
    }

    // =====================================================
    // ATUALIZA POSIÇÕES DOS DEDOS
    // =====================================================

    private void atualizarPosicoesDedos(
            MotionEvent evento) {

        for (int i = 0; i < evento.getPointerCount(); i++) {

            int id = evento.getPointerId(i);

            float y = obterYNaTela(evento, i);

            if (id == idDedoEsquerdo) {

                posicaoDedoEsquerdo = y;

            } else if (id == idDedoDireito) {

                posicaoDedoDireito = y;
            }
        }
    }

    // =====================================================
    // LIBERA UM DEDO
    // =====================================================

    private void liberarPonteiro(int id) {

        if (id == idDedoEsquerdo) {

            idDedoEsquerdo = -1;

            esquerdoPressionado = false;

            Log.d(TAG,
                    "Controle esquerdo liberado.");
        }

        if (id == idDedoDireito) {

            idDedoDireito = -1;

            direitoPressionado = false;

            Log.d(TAG,
                    "Controle direito liberado.");
        }
    }

    private void liberarTodosOsDedos() {

        idDedoEsquerdo = -1;
        idDedoDireito = -1;

        esquerdoPressionado = false;
        direitoPressionado = false;
    }

    // =====================================================
    // INICIA CONTROLE MANUAL
    // =====================================================

    private void verificarControleManual() {

        if (!esquerdoPressionado &&
                !direitoPressionado) {

            return;
        }

        if (movie == null) {
            return;
        }

        if (!controleManualAtivo) {

            controleManualAtivo = true;

            Log.d(TAG,
                    "CONTROLE MANUAL INICIADO.");

            pausarAnimacao();
        }
    }

    // =====================================================
    // ATUALIZA POSIÇÃO DOS CONTROLES
    // =====================================================

    private void atualizarControleManual() {

        if (!controleManualAtivo ||
                (!esquerdoPressionado &&
                        !direitoPressionado) ||
                movie == null) {

            return;
        }

        int altura = areaGif.getHeight();

        if (altura <= 0) return;

        int[] localizacao = new int[2];

        areaGif.getLocationOnScreen(localizacao);

        float posicaoMedia;

        if (esquerdoPressionado &&
                direitoPressionado) {

            posicaoMedia =
                    (posicaoDedoEsquerdo +
                            posicaoDedoDireito) / 2f;

        } else if (esquerdoPressionado) {

            posicaoMedia = posicaoDedoEsquerdo;

        } else {

            posicaoMedia = posicaoDedoDireito;
        }

        float posicaoLocal =
                posicaoMedia - localizacao[1];

        float metadeBotao =
                controleEsquerdo.getHeight() / 2f;

        float limiteInferior = metadeBotao;

        float limiteSuperior =
                altura - metadeBotao;

        if (limiteSuperior <= limiteInferior) {
            return;
        }

        posicaoLocal = Math.max(
                limiteInferior,
                Math.min(limiteSuperior, posicaoLocal)
        );

        float progresso =
                (posicaoLocal - metadeBotao) /
                        (altura - 2f * metadeBotao);

        progresso = Math.max(
                0f,
                Math.min(1f, progresso)
        );

        registrarProgressoMovimento(progresso);

        int duracao = obterDuracaoGif();

        posicaoMs = (long) (
                progresso * duracao
        );

        int centroY = (int) posicaoLocal;

        if (esquerdoPressionado &&
                direitoPressionado) {

            moverBotao(
                    controleEsquerdo,
                    centroY,
                    true
            );

            moverBotao(
                    controleDireito,
                    centroY,
                    false
            );

        } else if (esquerdoPressionado) {

            moverBotao(
                    controleEsquerdo,
                    centroY,
                    true
            );

        } else {

            moverBotao(
                    controleDireito,
                    centroY,
                    false
            );
        }

        desenharQuadro();

        enviarComandoControle(
                true,
                progresso
        );
    }

    // =====================================================
    // MOVIMENTAÇÃO DOS BOTÕES
    // =====================================================

    private void moverBotao(
            TextView botao,
            int centroY,
            boolean esquerdo) {

        if (botao == null ||
                botao.getLayoutParams() == null) {

            return;
        }

        FrameLayout.LayoutParams parametros =
                (FrameLayout.LayoutParams)
                        botao.getLayoutParams();

        parametros.gravity =
                Gravity.TOP |
                        (esquerdo
                                ? Gravity.START
                                : Gravity.END);

        parametros.topMargin = Math.max(
                0,
                centroY - botao.getHeight() / 2
        );

        botao.setLayoutParams(parametros);
    }

    // =====================================================
    // LIBERAÇÃO DO CONTROLE MANUAL
    // =====================================================

    private void verificarLiberacao() {

        if (esquerdoPressionado ||
                direitoPressionado) {

            atualizarControleManual();

            return;
        }

        boolean estavaAtivo = controleManualAtivo;

        controleManualAtivo = false;

        if (estavaAtivo) {

            Log.d(TAG,
                    "CONTROLE MANUAL ENCERRADO.");

            enviarComandoControle(false, 0f);
        }

        finalizarMovimentoSeLiberado();
    }

    // =====================================================
    // PAUSAR ANIMAÇÃO
    // =====================================================

    private void pausarAnimacao() {

        if (movie == null) return;

        if (reproduzindo) {

            int duracao = obterDuracaoGif();

            long decorrido =
                    SystemClock.uptimeMillis() -
                            inicioReproducao;

            posicaoMs = (
                    posicaoMs + decorrido
            ) % duracao;
        }

        reproduzindo = false;

        handler.removeCallbacks(atualizadorGif);
    }

    // =====================================================
    // RETOMAR ANIMAÇÃO
    // =====================================================

    private void iniciarAnimacao() {

        if (movie == null ||
                reproduzindo ||
                controleManualAtivo ||
                controleRemotoAtivo ||
                !atividadeDisponivel()) {

            return;
        }

        reproduzindo = true;

        inicioReproducao =
                SystemClock.uptimeMillis();

        handler.removeCallbacks(atualizadorGif);

        handler.post(atualizadorGif);
    }

    // =====================================================
    // DESENHO DO QUADRO
    // =====================================================

    private void desenharQuadro() {

        if (!atividadeDisponivel() ||
                movie == null ||
                areaGif.getWidth() <= 0 ||
                areaGif.getHeight() <= 0) {

            return;
        }

        int largura = areaGif.getWidth();

        int altura = areaGif.getHeight();

        if (quadroBitmap == null ||
                quadroBitmap.getWidth() != largura ||
                quadroBitmap.getHeight() != altura) {

            if (quadroBitmap != null &&
                    !quadroBitmap.isRecycled()) {

                quadroBitmap.recycle();
            }

            quadroBitmap = Bitmap.createBitmap(
                    largura,
                    altura,
                    Bitmap.Config.ARGB_8888
            );

            quadroCanvas = new Canvas(quadroBitmap);

            imagemGif.setImageBitmap(quadroBitmap);
        }

        quadroCanvas.drawColor(Color.BLACK);

        int larguraGif = movie.width();

        int alturaGif = movie.height();

        if (larguraGif <= 0 ||
                alturaGif <= 0) {

            return;
        }

        float escala = Math.min(
                (float) largura / larguraGif,
                (float) altura / alturaGif
        );

        float larguraFinal =
                larguraGif * escala;

        float alturaFinal =
                alturaGif * escala;

        float deslocamentoX =
                (largura - larguraFinal) / 2f;

        float deslocamentoY =
                (altura - alturaFinal) / 2f;

        quadroCanvas.save();

        quadroCanvas.translate(
                deslocamentoX,
                deslocamentoY
        );

        quadroCanvas.scale(
                escala,
                escala
        );

        movie.setTime((int) posicaoMs);

        movie.draw(
                quadroCanvas,
                0,
                0,
                pincel
        );

        quadroCanvas.restore();

        imagemGif.invalidate();
    }

    // =====================================================
    // CONSULTA À PLANILHA
    // =====================================================

    private void carregarPlanilha() {

        status.setText(
                "Consultando Google Planilhas..."
        );

        botaoProximo.setEnabled(false);

        executor.execute(() -> {

            HttpURLConnection conexao = null;

            try {

                String endereco =
                        "https://docs.google.com/spreadsheets/d/" +
                                PLANILHA_ID +
                                "/export?format=csv&gid=" + GID;

                URL url = new URL(endereco);

                conexao = (HttpURLConnection)
                        url.openConnection();

                conexao.setConnectTimeout(15000);
                conexao.setReadTimeout(15000);

                int codigoHttp = conexao.getResponseCode();

                if (codigoHttp != HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "Erro HTTP: " + codigoHttp
                    );
                }

                nomes.clear();
                links.clear();
                textosPlanilha.clear();

                try (
                        BufferedReader leitor =
                                new BufferedReader(
                                        new InputStreamReader(
                                                conexao.getInputStream(),
                                                "UTF-8"
                                        )
                                )
                ) {

                    String linha;

                    boolean primeiraLinha = true;

                    while ((linha = leitor.readLine()) != null) {

                        ArrayList<String> colunas =
                                separarCSV(linha);

                        if (primeiraLinha) {

                            primeiraLinha = false;

                            continue;
                        }

                        if (colunas.size() < 2) {
                            continue;
                        }

                        String nome =
                                colunas.get(0).trim();

                        String link =
                                colunas.get(1).trim();

                        String texto =
                                colunas.size() > 4
                                        ? colunas.get(4).trim()
                                        : "";

                        if (!nome.isEmpty() &&
                                !link.isEmpty()) {

                            nomes.add(nome);
                            links.add(link);
                            textosPlanilha.add(texto);
                        }
                    }
                }

                handler.post(() -> {

                    if (!atividadeDisponivel()) return;

                    indiceAtual = -1;

                    if (nomes.isEmpty()) {

                        status.setText(
                                "Nenhum GIF encontrado."
                        );

                        botaoProximo.setEnabled(false);

                    } else {

                        status.setText(
                                nomes.size() +
                                        " GIFs disponíveis."
                        );

                        botaoProximo.setEnabled(true);

                        proximoGif();
                    }
                });

            } catch (Exception e) {

                Log.e(TAG,
                        "Erro na planilha.",
                        e);

                handler.post(() -> {

                    if (!atividadeDisponivel()) return;

                    status.setText(
                            "Erro ao consultar planilha: " +
                                    e.getMessage()
                    );

                    botaoProximo.setEnabled(false);
                });

            } finally {

                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }

    // =====================================================
    // LEITURA CSV
    // =====================================================

    private ArrayList<String> separarCSV(String linha) {

        ArrayList<String> resultado =
                new ArrayList<>();

        StringBuilder campo =
                new StringBuilder();

        boolean dentroAspas = false;

        for (int i = 0; i < linha.length(); i++) {

            char caractere = linha.charAt(i);

            if (caractere == '"') {

                if (dentroAspas &&
                        i + 1 < linha.length() &&
                        linha.charAt(i + 1) == '"') {

                    campo.append('"');

                    i++;

                } else {

                    dentroAspas = !dentroAspas;
                }

            } else if (caractere == ',' &&
                    !dentroAspas) {

                resultado.add(campo.toString());

                campo.setLength(0);

            } else {

                campo.append(caractere);
            }
        }

        resultado.add(campo.toString());

        return resultado;
    }

    // =====================================================
    // PRÓXIMO GIF
    // =====================================================

    private void proximoGif() {

        if (!atividadeDisponivel() || carregando) {
            return;
        }

        if (nomes.isEmpty()) {

            status.setText(
                    "Nenhum GIF disponível."
            );

            return;
        }

        indiceAtual++;

        if (indiceAtual >= nomes.size()) {
            indiceAtual = 0;
        }

        baixarEExibir(indiceAtual);
    }

    // =====================================================
    // DOWNLOAD DO GIF PRINCIPAL
    // =====================================================

    private void baixarEExibir(int posicao) {

        if (!atividadeDisponivel()) return;

        if (posicao < 0 || posicao >= nomes.size()) {
            return;
        }

        String nome = nomes.get(posicao);

        String link = links.get(posicao);

        String id = extrairId(link);

        if (id == null) {

            status.setText(
                    "Link inválido: " + nome
            );

            return;
        }

        File arquivo = new File(
                pastaGifs,
                id + ".gif"
        );

        if (arquivo.exists() &&
                arquivo.length() > 0) {

            exibirGif(arquivo, nome);

            return;
        }

        carregando = true;

        botaoProximo.setEnabled(false);

        status.setText(
                "Baixando: " + nome
        );

        executor.execute(() -> {

            HttpURLConnection conexao = null;

            File temporario = new File(
                    pastaGifs,
                    id + ".part"
            );

            try {

                String endereco =
                        "https://drive.usercontent.google.com/download?id=" +
                                id +
                                "&export=download&confirm=t";

                conexao = (HttpURLConnection)
                        new URL(endereco).openConnection();

                conexao.setConnectTimeout(20000);
                conexao.setReadTimeout(30000);

                conexao.setInstanceFollowRedirects(true);

                int codigoHttp = conexao.getResponseCode();

                if (codigoHttp != HttpURLConnection.HTTP_OK) {

                    throw new Exception(
                            "Erro HTTP: " + codigoHttp
                    );
                }

                try (
                        InputStream entrada =
                                conexao.getInputStream();

                        FileOutputStream saida =
                                new FileOutputStream(temporario)
                ) {

                    byte[] buffer = new byte[8192];

                    int quantidade;

                    while ((quantidade =
                            entrada.read(buffer)) != -1) {

                        saida.write(buffer, 0, quantidade);
                    }

                    saida.flush();
                }

                if (temporario.length() == 0) {
                    throw new Exception("Arquivo vazio.");
                }

                // Verifica se o conteúdo recebido realmente
                // pode ser decodificado como GIF.

                try (
                        FileInputStream entradaValidacao =
                                new FileInputStream(temporario)
                ) {

                    Movie teste = Movie.decodeStream(
                            entradaValidacao
                    );

                    if (teste == null) {

                        throw new Exception(
                                "O arquivo recebido não é um GIF válido."
                        );
                    }
                }

                if (arquivo.exists() && !arquivo.delete()) {

                    throw new Exception(
                            "Não foi possível substituir o arquivo antigo."
                    );
                }

                if (!temporario.renameTo(arquivo)) {

                    throw new Exception(
                            "Erro ao salvar GIF."
                    );
                }

                handler.post(() -> {

                    if (!atividadeDisponivel()) return;

                    carregando = false;

                    botaoProximo.setEnabled(true);

                    exibirGif(arquivo, nome);
                });

            } catch (Exception e) {

                if (temporario.exists()) {
                    temporario.delete();
                }

                Log.e(TAG,
                        "Erro no download do GIF.",
                        e);

                handler.post(() -> {

                    if (!atividadeDisponivel()) return;

                    carregando = false;

                    botaoProximo.setEnabled(true);

                    status.setText(
                            "Erro no download: " +
                                    e.getMessage()
                    );
                });

            } finally {

                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }

    // =====================================================
    // EXTRAIR ID DO GOOGLE DRIVE
    // =====================================================

    private String extrairId(String link) {

        if (link == null || link.trim().isEmpty()) {
            return null;
        }

        try {

            String resultado = null;

            if (link.contains("/file/d/")) {

                String parte =
                        link.split("/file/d/")[1];

                resultado = parte.split("/")[0];

            } else if (link.contains("id=")) {

                String parte =
                        link.split("id=")[1];

                resultado = parte.split("&")[0];
            }

            if (resultado == null) {
                return null;
            }

            resultado = resultado.trim();

            if (resultado.isEmpty() ||
                    !resultado.matches("[a-zA-Z0-9_-]+")) {

                return null;
            }

            return resultado;

        } catch (Exception e) {

            Log.e(TAG,
                    "Não foi possível extrair o ID do Drive.",
                    e);

            return null;
        }
    }

    // =====================================================
    // EXIBIÇÃO DO GIF PRINCIPAL
    // =====================================================

    private void exibirGif(
            File arquivo,
            String nome) {

        if (!atividadeDisponivel()) return;

        // Cada nova solicitação invalida as decodificações
        // anteriores que ainda estejam em andamento.

        long geracaoAtual = ++geracaoExibicao;

        handler.removeCallbacks(atualizadorGif);

        handler.removeCallbacks(atualizarTextoPlanilha);

        reproduzindo = false;

        // Se o usuário avançar enquanto segura um controle,
        // envia a liberação antes de limpar os estados locais.

        if (controleManualAtivo) {
            enviarComandoControle(false, 0f);
        }

        movie = null;

        posicaoMs = 0;

        inicioExibicaoGif = 0;

        textoPlanilha.setVisibility(View.GONE);

        controleManualAtivo = false;

        controleRemotoAtivo = false;

        reiniciarContagemMovimentos();

        limparIdentificacaoDedos();

        areaGif.post(() -> {

            if (!atividadeDisponivel() ||
                    geracaoAtual != geracaoExibicao) {

                return;
            }

            int centro = areaGif.getHeight() / 2;

            moverBotao(
                    controleEsquerdo,
                    centro,
                    true
            );

            moverBotao(
                    controleDireito,
                    centro,
                    false
            );
        });

        executor.execute(() -> {

            try {

                Movie novoMovie;

                try (
                        FileInputStream entrada =
                                new FileInputStream(arquivo)
                ) {

                    novoMovie = Movie.decodeStream(entrada);
                }

                if (novoMovie == null) {

                    throw new Exception(
                            "Não foi possível decodificar o GIF."
                    );
                }

                Movie resultado = novoMovie;

                handler.post(() -> {

                    if (!atividadeDisponivel() ||
                            geracaoAtual != geracaoExibicao) {

                        return;
                    }

                    movie = resultado;

                    posicaoMs = 0;

                    String texto = "";

                    if (indiceAtual >= 0 &&
                            indiceAtual < textosPlanilha.size()) {

                        texto = textosPlanilha.get(indiceAtual);
                    }

                    textoPlanilha.setText(texto);

                    textoPlanilha.setVisibility(View.GONE);

                    areaGif.post(() -> {

                        if (!atividadeDisponivel() ||
                                geracaoAtual != geracaoExibicao) {

                            return;
                        }

                        desenharQuadro();

                        iniciarAnimacao();

                        inicioExibicaoGif =
                                SystemClock.uptimeMillis();

                        handler.removeCallbacks(
                                atualizarTextoPlanilha
                        );

                        handler.post(
                                atualizarTextoPlanilha
                        );
                    });

                    status.setText(
                            nome + " | " +
                                    (indiceAtual + 1) +
                                    "/" + nomes.size()
                    );
                });

            } catch (Exception e) {

                Log.e(TAG,
                        "Erro ao abrir GIF.",
                        e);

                handler.post(() -> {

                    if (!atividadeDisponivel() ||
                            geracaoAtual != geracaoExibicao) {

                        return;
                    }

                    // Remove o arquivo inválido do cache.
                    // Assim, uma nova tentativa poderá baixá-lo novamente.

                    if (arquivo.exists()) {
                        arquivo.delete();
                    }

                    textoPlanilha.setVisibility(View.GONE);

                    status.setText(
                            "Erro ao abrir GIF: " +
                                    e.getMessage()
                    );
                });
            }
        });
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    @Override
    protected void onDestroy() {

        atividadeEncerrada = true;

        // Invalida qualquer decodificação ainda pendente.

        geracaoExibicao++;

        handler.removeCallbacksAndMessages(null);

        reproduzindo = false;

        movie = null;

        controleManualAtivo = false;

        controleRemotoAtivo = false;

        reiniciarContagemMovimentos();

        limparIdentificacaoDedos();

        /*
         * NÃO fechar o WebSocket aqui.
         *
         * A conexão pertence à ConexaoActivity e deve
         * continuar ativa enquanto a sessão estiver aberta.
         */

        if (servicoConexao != null) {
            servicoConexao.definirCallback(null);
        }

        if (quadroBitmap != null) {

            if (!quadroBitmap.isRecycled()) {
                quadroBitmap.recycle();
            }

            quadroBitmap = null;

            quadroCanvas = null;
        }

        executor.shutdownNow();

        super.onDestroy();
    }
}