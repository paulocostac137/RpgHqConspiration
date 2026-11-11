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

import com.bumptech.glide.Glide;

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

    private static final long INTERVALO_GIF_MS = 7000;
    private static final long TEMPO_TEXTO_MS = 5000;
    private static final long INTERVALO_ATUALIZACAO_MS = 50;

    // =====================================================
    // COMPONENTES
    // =====================================================

    private ImageView imagemGif;
    private ImageView imagemGifC;
    private ImageView imagemGifD;

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

    private final ArrayList<String> linksColunaC =
            new ArrayList<>();

    private final ArrayList<String> linksColunaD =
            new ArrayList<>();

    private final ArrayList<String> textosColunaE =
            new ArrayList<>();

    // =====================================================
    // EXECUÇÃO
    // =====================================================

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    /*
     * IMPORTANTE:
     *
     * Não existe mais um WebSocket próprio nesta Activity.
     * A conexão será reutilizada da ConexaoActivity.
     */

    private ServicoConexao servicoConexao;

    private boolean atividadeEncerrada = false;

    // Identifica se o aplicativo está funcionando sem servidor.
    private boolean modoIndividual = false;

    private boolean controleRemotoAtivo = false;

    private boolean carregando = false;

    private File pastaGifs;

    private int indiceAtual = -1;

    // =====================================================
    // ANIMAÇÃO
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
    // TROCA AUTOMÁTICA
    // =====================================================

    private final Runnable trocaAutomaticaGif = new Runnable() {

        @Override
        public void run() {

            if (atividadeEncerrada ||
                    isFinishing() ||
                    isDestroyed()) {
                return;
            }

            if (controleManualAtivo ||
                    controleRemotoAtivo) {

                handler.postDelayed(this, 250);
                return;
            }

            if (!nomes.isEmpty() && !carregando) {
                proximoGif();
            } else {
                handler.postDelayed(
                        this,
                        INTERVALO_GIF_MS
                );
            }
        }
    };

    private void iniciarTrocaAutomatica() {

        handler.removeCallbacks(trocaAutomaticaGif);

        handler.postDelayed(
                trocaAutomaticaGif,
                INTERVALO_GIF_MS
        );
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

        if (hasFocus) {
            ativarModoImersivo();
        }
    }

    // =====================================================
    // CONEXÃO COMPARTILHADA
    // =====================================================

    private void conectarServidor() {

        /*
         * Recupera exatamente a mesma conexão criada
         * pela ConexaoActivity.
         */

        servicoConexao = ConexaoActivity.conexao;

        if (servicoConexao == null) {

            status.setText(
                    "ERRO: conexão com o servidor não encontrada."
            );

            Log.e(TAG,
                    "ServicoConexao não foi inicializado.");

            return;
        }

        /*
         * Confere se a conexão continua aberta.
         */

        if (!servicoConexao.estaConectado()) {

            status.setText(
                    "ERRO: conexão com o servidor foi perdida."
            );

            Log.e(TAG,
                    "A conexão compartilhada não está ativa.");

            return;
        }

        /*
         * Substitui o callback da tela de conexão
         * pelo callback da tela principal.
         *
         * A conexão WebSocket permanece a mesma.
         */

        servicoConexao.definirCallback(
                new ServicoConexao.Callback() {

                    @Override
                    public void onConectado() {

                        if (atividadeEncerrada) return;

                        Log.d(TAG,
                                "Conexão compartilhada ativa.");

                        status.setText(
                                "Conectado ao servidor."
                        );
                    }

                    @Override
                    public void onDesconectado(String motivo) {

                        if (atividadeEncerrada) return;

                        Log.w(TAG,
                                "Servidor desconectado: " + motivo);

                        controleRemotoAtivo = false;

                        if (movie != null &&
                                !controleManualAtivo) {

                            iniciarAnimacao();
                        }

                        status.setText(
                                "Servidor desconectado."
                        );
                    }

                    @Override
                    public void onMensagem(JSONObject mensagem) {

                        if (atividadeEncerrada) return;

                        processarMensagemServidor(mensagem);
                    }

                    @Override
                    public void onErro(String erro) {

                        if (atividadeEncerrada) return;

                        Log.e(TAG,
                                "Erro de conexão: " + erro);

                        status.setText(
                                "Erro de conexão: " + erro
                        );
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

                if (movie != null &&
                        !controleManualAtivo) {

                    iniciarAnimacao();
                }

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

            if (movie != null &&
                    !controleManualAtivo) {

                iniciarAnimacao();
            }

            return;
        }

        if (movie == null) {
            return;
        }

        /*
         * Não bloqueamos mais o comando remoto quando
         * existe um dedo pressionando localmente.
         *
         * Isso permite que os dois aparelhos continuem
         * recebendo atualizações durante movimentos simultâneos.
         */

        float progressoRecebido =
                (float) dados.optDouble("progress", 0);

        progressoRecebido = Math.max(
                0f,
                Math.min(1f, progressoRecebido)
        );

        controleRemotoAtivo = true;

        pausarAnimacao();

        int duracao = movie.duration();

        if (duracao <= 0) {
            duracao = 1000;
        }

        posicaoMs = (long) (
                progressoRecebido * duracao
        );

        final float progressoFinal = progressoRecebido;

        areaGif.post(() -> {

            if (atividadeEncerrada || movie == null) {
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

            /*
             * O parceiro movimenta os dois controles
             * na mesma posição.
             */

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

        /*
         * O envio agora passa exclusivamente pelo
         * ServicoConexao compartilhado.
         */

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
    // ATUALIZAÇÃO AUTOMÁTICA DO GIF
    // =====================================================

    private final Runnable atualizadorGif = new Runnable() {

        @Override
        public void run() {

            if (movie == null || !reproduzindo) {
                return;
            }

            int duracao = movie.duration();

            if (duracao <= 0) {
                duracao = 1000;
            }

            long agora = SystemClock.uptimeMillis();

            long decorrido = agora - inicioReproducao;

            posicaoMs = (
                    posicaoMs + decorrido
            ) % duracao;

            inicioReproducao = agora;

            desenharQuadro();

            handler.postDelayed(this, 30);
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

            if (atividadeEncerrada) return;

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

                    if (atividadeEncerrada ||
                            isFinishing() ||
                            isDestroyed()) {

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
                            decorrido < INTERVALO_GIF_MS) {

                        textoPlanilha.setVisibility(View.VISIBLE);

                    } else {

                        textoPlanilha.setVisibility(View.GONE);
                    }

                    if (decorrido < INTERVALO_GIF_MS) {

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
        imagemGifC = findViewById(R.id.imagemGifC);
        imagemGifD = findViewById(R.id.imagemGifD);

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

        if (!pastaGifs.exists()) {
            pastaGifs.mkdirs();
        }

        botaoProximo.setOnClickListener(v -> {

            handler.removeCallbacks(trocaAutomaticaGif);

            proximoGif();
        });

        configurarControles();

        /*
         * Identifica o modo de funcionamento.
         *
         * Individual: não utiliza conexão com servidor.
         * Multiplayer: reutiliza a conexão já estabelecida.
         */

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

        int duracao = movie.duration();

        if (duracao <= 0) {
            duracao = 1000;
        }

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

        /*
         * Envia o progresso pela conexão compartilhada.
         */

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

        if (movie != null &&
                !controleRemotoAtivo) {

            iniciarAnimacao();
        }
    }

    // =====================================================
    // PAUSAR ANIMAÇÃO
    // =====================================================

    private void pausarAnimacao() {

        if (movie == null) return;

        if (reproduzindo) {

            int duracao = movie.duration();

            if (duracao <= 0) {
                duracao = 1000;
            }

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
                controleRemotoAtivo) {

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

        if (movie == null ||
                areaGif.getWidth() <= 0 ||
                areaGif.getHeight() <= 0) {

            return;
        }

        int largura = areaGif.getWidth();

        int altura = areaGif.getHeight();

        if (quadroBitmap == null ||
                quadroBitmap.getWidth() != largura ||
                quadroBitmap.getHeight() != altura) {

            if (quadroBitmap != null) {
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

                BufferedReader leitor =
                        new BufferedReader(
                                new InputStreamReader(
                                        conexao.getInputStream(),
                                        "UTF-8"
                                )
                        );

                String linha;

                boolean primeiraLinha = true;

                nomes.clear();
                links.clear();

                linksColunaC.clear();
                linksColunaD.clear();

                textosColunaE.clear();

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

                    String linkC =
                            colunas.size() > 2
                                    ? colunas.get(2).trim()
                                    : "";

                    String linkD =
                            colunas.size() > 3
                                    ? colunas.get(3).trim()
                                    : "";

                    String texto =
                            colunas.size() > 4
                                    ? colunas.get(4).trim()
                                    : "";

                    if (!nome.isEmpty() &&
                            !link.isEmpty()) {

                        nomes.add(nome);
                        links.add(link);

                        linksColunaC.add(linkC);
                        linksColunaD.add(linkD);

                        textosColunaE.add(texto);
                    }
                }

                leitor.close();

                handler.post(() -> {

                    if (atividadeEncerrada) return;

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

                    if (atividadeEncerrada) return;

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

        if (carregando) return;

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
    // GIFS SECUNDÁRIOS
    // =====================================================

    private void atualizarGifsSecundarios(
            int posicao) {

        carregarGifSecundario(
                posicao,
                linksColunaC.get(posicao),
                imagemGifC
        );

        carregarGifSecundario(
                posicao,
                linksColunaD.get(posicao),
                imagemGifD
        );
    }

    private void carregarGifSecundario(
            int posicao,
            String link,
            ImageView imagem) {

        imagem.setImageDrawable(null);

        if (link == null ||
                link.trim().isEmpty()) {

            imagem.setVisibility(View.INVISIBLE);

            return;
        }

        String id = extrairId(link);

        if (id == null) {

            imagem.setVisibility(View.INVISIBLE);

            return;
        }

        imagem.setVisibility(View.VISIBLE);

        File arquivo = new File(
                pastaGifs,
                id + ".gif"
        );

        if (arquivo.exists() &&
                arquivo.length() > 0) {

            Glide.with(this)
                    .asGif()
                    .load(arquivo)
                    .into(imagem);

            return;
        }

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

                if (conexao.getResponseCode() != 200) {
                    throw new Exception("Erro HTTP");
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
                }

                if (temporario.length() == 0) {
                    throw new Exception("Arquivo vazio");
                }

                if (!temporario.renameTo(arquivo)) {
                    throw new Exception("Erro ao salvar GIF");
                }

                handler.post(() -> {

                    if (atividadeEncerrada ||
                            isFinishing() ||
                            isDestroyed()) {

                        return;
                    }

                    if (indiceAtual != posicao) return;

                    Glide.with(this)
                            .asGif()
                            .load(arquivo)
                            .into(imagem);
                });

            } catch (Exception e) {

                temporario.delete();

                Log.e(TAG,
                        "Erro no GIF secundário: " + id,
                        e);

                handler.post(() -> {

                    if (atividadeEncerrada) return;

                    if (indiceAtual == posicao) {

                        imagem.setImageDrawable(null);

                        imagem.setVisibility(View.INVISIBLE);
                    }
                });

            } finally {

                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }

    // =====================================================
    // DOWNLOAD DO GIF PRINCIPAL
    // =====================================================

    private void baixarEExibir(int posicao) {

        atualizarGifsSecundarios(posicao);

        String nome = nomes.get(posicao);

        String link = links.get(posicao);

        String id = extrairId(link);

        if (id == null) {

            status.setText(
                    "Link inválido: " + nome
            );

            iniciarTrocaAutomatica();

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

                if (conexao.getResponseCode() != 200) {

                    throw new Exception(
                            "Erro HTTP: " +
                                    conexao.getResponseCode()
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
                }

                if (temporario.length() == 0) {
                    throw new Exception("Arquivo vazio.");
                }

                if (!temporario.renameTo(arquivo)) {
                    throw new Exception("Erro ao salvar GIF.");
                }

                handler.post(() -> {

                    if (atividadeEncerrada) return;

                    carregando = false;

                    botaoProximo.setEnabled(true);

                    exibirGif(arquivo, nome);
                });

            } catch (Exception e) {

                temporario.delete();

                Log.e(TAG,
                        "Erro no download do GIF.",
                        e);

                handler.post(() -> {

                    if (atividadeEncerrada) return;

                    carregando = false;

                    botaoProximo.setEnabled(true);

                    status.setText(
                            "Erro no download: " +
                                    e.getMessage()
                    );

                    iniciarTrocaAutomatica();
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

        try {

            if (link.contains("/file/d/")) {

                String parte =
                        link.split("/file/d/")[1];

                return parte.split("/")[0];
            }

            if (link.contains("id=")) {

                String parte =
                        link.split("id=")[1];

                return parte.split("&")[0];
            }

            return null;

        } catch (Exception e) {

            return null;
        }
    }

    // =====================================================
    // EXIBIÇÃO DO GIF PRINCIPAL
    // =====================================================

    private void exibirGif(
            File arquivo,
            String nome) {

        handler.removeCallbacks(atualizadorGif);

        handler.removeCallbacks(atualizarTextoPlanilha);

        handler.removeCallbacks(trocaAutomaticaGif);

        reproduzindo = false;

        movie = null;

        posicaoMs = 0;

        inicioExibicaoGif = 0;

        textoPlanilha.setVisibility(View.GONE);

        controleManualAtivo = false;

        controleRemotoAtivo = false;

        limparIdentificacaoDedos();

        areaGif.post(() -> {

            if (atividadeEncerrada) return;

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

                    if (atividadeEncerrada) return;

                    movie = resultado;

                    posicaoMs = 0;

                    String texto = "";

                    if (indiceAtual >= 0 &&
                            indiceAtual < textosColunaE.size()) {

                        texto = textosColunaE.get(indiceAtual);
                    }

                    textoPlanilha.setText(texto);

                    textoPlanilha.setVisibility(View.GONE);

                    areaGif.post(() -> {

                        if (atividadeEncerrada) return;

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

                        iniciarTrocaAutomatica();
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

                    if (atividadeEncerrada) return;

                    textoPlanilha.setVisibility(View.GONE);

                    status.setText(
                            "Erro ao abrir GIF: " +
                                    e.getMessage()
                    );

                    iniciarTrocaAutomatica();
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

        handler.removeCallbacksAndMessages(null);

        reproduzindo = false;

        movie = null;

        controleManualAtivo = false;

        controleRemotoAtivo = false;

        limparIdentificacaoDedos();

        /*
         * NÃO fechar o WebSocket aqui.
         *
         * A conexão pertence à ConexaoActivity e deve
         * continuar ativa enquanto a sessão estiver aberta.
         *
         * Apenas removemos o callback desta Activity para
         * evitar que o serviço tente atualizar uma tela destruída.
         */

        if (servicoConexao != null) {
            servicoConexao.definirCallback(null);
        }

        if (imagemGifC != null) {
            Glide.with(this).clear(imagemGifC);
        }

        if (imagemGifD != null) {
            Glide.with(this).clear(imagemGifD);
        }

        if (imagemGif != null) {
            Glide.with(this).clear(imagemGif);
        }

        if (quadroBitmap != null) {

            quadroBitmap.recycle();

            quadroBitmap = null;

            quadroCanvas = null;
        }

        executor.shutdownNow();

        super.onDestroy();
    }
}