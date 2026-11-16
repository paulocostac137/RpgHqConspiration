package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import android.util.Log;

import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.File;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RPGHQ_SYNC";

    private MainUiState mainUiState;

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

    private TextView status;
    private TextView textoPlanilha;

    private TextView controleEsquerdo;
    private TextView controleDireito;

    private FrameLayout areaGif;

    // =====================================================
    // DADOS DA PLANILHA
    // =====================================================

    private final GifCatalog gifCatalog = new GifCatalog();
    private SpreadsheetSource spreadsheetSource;
    private GifDownloadSource gifDownloadSource;

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

    // Indica se o aplicativo está funcionando em modo individual, sem conexão com o servidor.
    private boolean modoIndividual = false;

    private File pastaGifs;

    private final GifSessionState gifSessionState = new GifSessionState();
    private SharedConnectionSession sharedConnectionSession;

    // =====================================================
    // ANIMAÇÃO
    // =====================================================

    private GifPlayer gifPlayer;

    private GifTiming gifTiming;

    private GifRemoteSession gifRemoteSession;

    // =====================================================
    // CONTROLES MULTITOQUE
    // =====================================================

    private GifControls gifControls;

    // =====================================================
    // TROCA AUTOMÁTICA
    // =====================================================

    private GifMediaFlow gifMediaFlow;

    private void iniciarTrocaAutomatica() {
        if (gifTiming != null) {
            gifTiming.iniciarTrocaAutomatica();
        }
    }

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
    // CONEXÃO COMPARTILHADA
    // =====================================================

    private void conectarServidor() {

        sharedConnectionSession = new SharedConnectionSession(
                new SharedConnectionSession.Callback() {
                    @Override
                    public void onConectado() {
                        if (atividadeEncerrada) return;

                        Log.d(TAG, "Conexão compartilhada ativa.");
                        status.setText("Conectado ao servidor.");
                    }

                    @Override
                    public void onDesconectado(String motivo) {
                        if (atividadeEncerrada) return;

                        Log.w(TAG, "Servidor desconectado: " + motivo);
                        gifSessionState.setRemotoAtivo(false);

                        if (gifPlayer != null &&
                                gifPlayer.getMovie() != null &&
                                !gifSessionState.isManualAtivo()) {
                            iniciarAnimacao();
                        }

                        status.setText("Servidor desconectado.");
                    }

                    @Override
                    public void onMensagem(JSONObject mensagem) {
                        if (atividadeEncerrada) return;
                        processarMensagemServidor(mensagem);
                    }

                    @Override
                    public void onErro(String erro) {
                        if (atividadeEncerrada) return;

                        Log.e(TAG, "Erro de conexão: " + erro);
                        status.setText("Erro de conexão: " + erro);
                    }
                }
        );

        servicoConexao = sharedConnectionSession.bindAndGet();

        if (servicoConexao == null) {
            status.setText("ERRO: conexão com o servidor não encontrada.");
            Log.e(TAG, "ServicoConexao não foi inicializado.");
            return;
        }

        if (!servicoConexao.estaConectado()) {
            status.setText("ERRO: conexão com o servidor foi perdida.");
            Log.e(TAG, "A conexão compartilhada não está ativa.");
            return;
        }

        Log.d(TAG, "MainActivity reutilizando a conexão existente.");
        status.setText("Conectado. Aguardando comandos...");
    }

    // =====================================================
    // MENSAGENS RECEBIDAS
    // =====================================================

    private void processarMensagemServidor(JSONObject dados) {
        if (gifRemoteSession != null) {
            gifRemoteSession.processarMensagemServidor(dados);
        }
    }

    // =====================================================
    // CONTROLE RECEBIDO DO PARCEIRO
    // =====================================================

    private void processarControleRemoto(JSONObject dados) {
        if (gifRemoteSession != null) {
            gifRemoteSession.processarControleRemoto(dados);
        }
    }

    // =====================================================
    // ENVIO DE COMANDOS AO PARCEIRO
    // =====================================================

    private void enviarComandoControle(
            boolean ativo,
            float progresso) {

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


    // =====================================================
    // TEXTO DA PLANILHA
    // =====================================================

    private void configurarTextoPlanilha() {
        if (mainUiState != null) {
            mainUiState.configurarTextoPlanilha();
        }
    }


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

        controleEsquerdo =
                findViewById(R.id.controleEsquerdo);

        controleDireito =
                findViewById(R.id.controleDireito);

        areaGif = findViewById(R.id.areaGif);
        areaGif.addOnLayoutChangeListener((view, left, top, right, bottom,
                                           oldLeft, oldTop, oldRight, oldBottom) ->
                ajustarAfastamentoDosControles());

        configurarFluxoLinear();

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

        iniciarFluxoLinear();
    }

    // =====================================================
    // ORQUESTRAÇÃO LINEAR DOS MÓDULOS
    // =====================================================

    private void configurarFluxoLinear() {
        spreadsheetSource = new SpreadsheetLoader();
        gifDownloadSource = new DriveGifRepository();
        gifPlayer = new GifPlayer(handler, imagemGif, areaGif);

        gifRemoteSession = new GifRemoteSession(
                new GifRemoteSession.Callback() {
                    @Override
                    public void onStatusUpdated(String mensagem) {
                        status.setText(mensagem);
                    }

                    @Override
                    public void onPauseAnimation() {
                        pausarAnimacao();
                    }

                    @Override
                    public void onResumeAnimationIfAllowed() {
                        if (gifPlayer != null &&
                                gifPlayer.getMovie() != null &&
                                !gifSessionState.isManualAtivo()) {
                            iniciarAnimacao();
                        }
                    }

                    @Override
                    public void onRequestRenderFrame() {
                        desenharQuadro();
                    }

                    @Override
                    public void onApplyRemoteProgress(float progressoFinal) {
                        areaGif.post(() -> {
                            if (atividadeEncerrada ||
                                    gifPlayer == null ||
                                    gifPlayer.getMovie() == null ||
                                    areaGif == null ||
                                    controleEsquerdo == null ||
                                    controleDireito == null) {
                                return;
                            }

                            int altura = areaGif.getHeight();
                            int alturaBotao = controleEsquerdo.getHeight();

                            if (altura <= 0 || alturaBotao <= 0) {
                                return;
                            }

                            float metadeBotao = alturaBotao / 2f;
                            int centroY = (int) (
                                    metadeBotao +
                                            progressoFinal *
                                                    (altura - 2f * metadeBotao)
                            );

                            moverBotao(controleEsquerdo, centroY);
                            moverBotao(controleDireito, centroY);
                            desenharQuadro();
                        });
                    }

                    @Override
                    public void onEnviarComandoControle(boolean ativo, float progresso) {
                        enviarComandoControle(ativo, progresso);
                    }
                },
                gifSessionState,
                gifPlayer
        );

        gifTiming = new GifTiming(
                handler,
                INTERVALO_GIF_MS,
                TEMPO_TEXTO_MS,
                INTERVALO_ATUALIZACAO_MS,
                new GifTiming.Callback() {
                    @Override
                    public void onAutoAdvanceRequested() {
                        if (atividadeEncerrada ||
                                isFinishing() ||
                                isDestroyed()) {
                            return;
                        }

                        if (gifSessionState.isManualAtivo() || gifSessionState.isRemotoAtivo()) {
                            return;
                        }

                        if (gifMediaFlow != null && !gifCatalog.isEmpty() && !gifSessionState.isCarregando()) {
                            gifMediaFlow.proximo();
                        }
                    }

                    @Override
                    public void onTextVisibilityChanged(boolean visible) {
                        if (textoPlanilha != null) {
                            textoPlanilha.setVisibility(
                                    visible ? View.VISIBLE : View.GONE
                            );
                        }
                    }
                }
        );

        textoPlanilha = findViewById(R.id.textoPlanilha);

        mainUiState = new MainUiState(
                status,
                textoPlanilha,
                areaGif
        );

        configurarTextoPlanilha();

        pastaGifs = new File(getFilesDir(), "gifs");

        if (!pastaGifs.exists()) {
            pastaGifs.mkdirs();
        }

        gifMediaFlow = new GifMediaFlow(
                handler,
                executor,
                pastaGifs,
                gifCatalog,
                gifPlayer,
                gifTiming,
                gifSessionState,
                new GifMediaFlow.Callback() {
                    @Override
                    public void onStatusChanged(String mensagem) {
                        status.setText(mensagem);
                    }

                    @Override
                    public void onTextoPlanilhaChanged(String texto) {
                        textoPlanilha.setText(texto);
                    }

                    @Override
                    public void onRequestDesenharQuadro() {
                        desenharQuadro();
                    }

                    @Override
                    public void onRequestIniciarAnimacao() {
                        iniciarAnimacao();
                    }

                    @Override
                    public void onRequestPausarAnimacao() {
                        pausarAnimacao();
                    }

                    @Override
                    public void onRequestResetarControles() {
                        limparIdentificacaoDedos();
                    }

                    @Override
                    public void onRequestCentralizarControles() {
                        areaGif.post(() -> {
                            if (atividadeEncerrada) return;

                            int centro = areaGif.getHeight() / 2;

                            moverBotao(controleEsquerdo, centro);
                            moverBotao(controleDireito, centro);
                        });
                    }
                }
        );

        gifControls = new GifControls(areaGif, controleEsquerdo, controleDireito);
        gifControls.configurar();
        gifControls.setOnManualProgressChanged(progresso -> {
            if (gifPlayer != null && gifPlayer.getMovie() != null) {
                int duracao = gifPlayer.getMovie().duration();
                if (duracao <= 0) {
                    duracao = 1000;
                }
                gifPlayer.setPosicaoMs((long) (progresso * duracao));
                desenharQuadro();
                enviarComandoControle(true, progresso);
            }
        });

        configurarControles();
    }

    private void iniciarFluxoLinear() {
        status.setText("Consultando Google Planilhas...");
        carregarPlanilha();
    }

    private void processarProximoItemNaCadeia() {
        if (gifMediaFlow != null) {
            gifMediaFlow.proximo();
        }
    }

    // =====================================================
    // CONFIGURAÇÃO DOS CONTROLES
    // =====================================================

    private void configurarControles() {

        View.OnTouchListener listenerMultitoque =
                (view, evento) -> {
                    processarEventoMultitoque(view, evento);
                    return true;
                };

        controleEsquerdo.setOnTouchListener(listenerMultitoque);
        controleDireito.setOnTouchListener(listenerMultitoque);
    }

    // =====================================================
    // PROCESSAMENTO MULTITOQUE
    // =====================================================

    private void processarEventoMultitoque(
            View view,
            MotionEvent evento) {

        if (gifControls == null) {
            return;
        }

        gifControls.processarEventoMultitoque(view, evento);
        gifControls.atualizarPosicoesDedo();

        if (gifControls.isManualAtivo() &&
                gifPlayer != null &&
                gifPlayer.getMovie() != null) {

            if (!gifSessionState.isManualAtivo()) {
                gifSessionState.setManualAtivo(true);
                pausarAnimacao();
            }

            return;
        }

        if (gifSessionState.isManualAtivo() && !gifControls.isManualAtivo()) {
            gifSessionState.setManualAtivo(false);
            enviarComandoControle(false, 0f);
            if (gifPlayer != null && gifPlayer.getMovie() != null && !gifSessionState.isRemotoAtivo()) {
                iniciarAnimacao();
            }
        }
    }

    // =====================================================
    // IDENTIFICAÇÃO DOS DEDOS
    // =====================================================

    private void limparIdentificacaoDedos() {

        if (gifControls != null) {
            gifControls.resetar();
        }

        gifSessionState.setManualAtivo(false);
    }

    // =====================================================
    // MOVIMENTAÇÃO DOS BOTÕES
    // =====================================================

    private void ajustarAfastamentoDosControles() {
        float afastamento = areaGif.getWidth() * 0.07f;
        float direcaoInicio = areaGif.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
                ? -1f
                : 1f;

        controleEsquerdo.setTranslationX(direcaoInicio * afastamento);
        controleDireito.setTranslationX(-direcaoInicio * afastamento);
    }

    private void moverBotao(
            TextView botao,
            int centroY) {

        if (botao == null ||
                botao.getHeight() <= 0) {
            return;
        }

        float centroInicial = botao.getTop() + botao.getHeight() / 2f;
        botao.setTranslationY(centroY - centroInicial);
    }

    // =====================================================
    // PAUSAR ANIMAÇÃO
    // =====================================================

    private void pausarAnimacao() {

        if (gifPlayer != null) {
            gifPlayer.pausar();
        }
    }

    // =====================================================
    // RETOMAR ANIMAÇÃO
    // =====================================================

    private void iniciarAnimacao() {

        if (gifPlayer == null ||
                gifSessionState.isManualAtivo() ||
                gifSessionState.isRemotoAtivo()) {

            return;
        }

        gifPlayer.iniciarAnimacao();
    }

    // =====================================================
    // DESENHO DO QUADRO
    // =====================================================

    private void desenharQuadro() {

        if (gifPlayer != null) {
            gifPlayer.desenharQuadro();
        }
    }

    // =====================================================
    // CONSULTA À PLANILHA
    // =====================================================

    private void carregarPlanilha() {

        status.setText(
                "Consultando Google Planilhas..."
        );

        spreadsheetSource.carregarPlanilha(
                handler,
                executor,
                new SpreadsheetSource.Callback() {
                    @Override
                    public void onSuccess(ArrayList<GifItem> itensCarregados) {

                        if (atividadeEncerrada) return;

                        gifCatalog.setItems(itensCarregados);

                        if (gifCatalog.isEmpty()) {

                            status.setText(
                                    "Nenhum item encontrado."
                            );

                        } else {

                            status.setText(
                                    gifCatalog.tamanho() +
                                            " itens disponíveis."
                            );

                            processarProximoItemNaCadeia();
                        }
                    }

                    @Override
                    public void onError(String mensagem, Exception erro) {

                        if (atividadeEncerrada) return;

                        Log.e(TAG, "Erro na planilha.", erro);

                        status.setText(mensagem);
                    }
                }
        );
    }


    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    @Override
    protected void onDestroy() {

        atividadeEncerrada = true;

        handler.removeCallbacksAndMessages(null);

        if (gifTiming != null) {
            gifTiming.limpar();
        }

        if (gifPlayer != null) {
            gifPlayer.limpar();
        }

        gifSessionState.resetarControles();

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

        if (imagemGif != null) {
            Glide.with(this).clear(imagemGif);
        }

        executor.shutdownNow();

        super.onDestroy();
    }
}