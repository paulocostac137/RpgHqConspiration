package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.graphics.Movie;
import android.graphics.Paint;

import android.util.Log;

import android.view.View;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.File;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RPGHQ_SYNC";

    // =====================================================
    // TEMPOS
    // =====================================================

    private static final long INTERVALO_ANIMACAO_MS = 30;

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

    // =====================================================
    // REPOSITÓRIOS E COMUNICAÇÃO
    // =====================================================

    private final PlanilhaRepository planilhaRepository =
            new PlanilhaRepository();

    private GifRepository gifRepository;

    private GerenciadorComunicacaoServidor
            gerenciadorComunicacaoServidor;

    // =====================================================
    // GERENCIADOR DE CARREGAMENTO DA PLANILHA
    // =====================================================

    private GerenciadorCarregamentoPlanilha
            gerenciadorCarregamentoPlanilha;

    // =====================================================
    // GERENCIADOR DE CARREGAMENTO DOS GIFS
    // =====================================================

    private GerenciadorCarregamentoGif
            gerenciadorCarregamentoGif;

    // =====================================================
    // GERENCIADOR DE DECODIFICAÇÃO DOS GIFS
    // =====================================================

    private GerenciadorDecodificacaoGif
            gerenciadorDecodificacaoGif;

    // Nome do GIF solicitado ao gerenciador.
    private String nomeGifSolicitado = "";

    // =====================================================
    // GERENCIADOR DE ESTADO DOS CONTROLES
    // =====================================================

    private GerenciadorEstadoControles gerenciadorEstadoControles;

    // =====================================================
    // GERENCIADOR DE MENSAGENS
    // =====================================================

    private final GerenciadorMensagens gerenciadorMensagens =
            new GerenciadorMensagens(
                    new GerenciadorMensagens.Listener() {

                        @Override
                        public void onJoined(int usuarios) {

                            atualizarVisibilidadeControles();

                            status.setText(
                                    usuarios == 1
                                            ? "Conectado. Aguardando parceiro..."
                                            : "Conectado! Dois usuários na sala."
                            );
                        }

                        @Override
                        public void onPeerConnected() {

                            status.setText(
                                    "PARCEIRO CONECTADO!"
                            );

                            Log.d(
                                    TAG,
                                    "O parceiro entrou na sala."
                            );
                        }

                        @Override
                        public void onPeerDisconnected() {

                            controleRemotoAtivo = false;

                            finalizarMovimentoSeLiberado();

                            status.setText(
                                    "Parceiro desconectado."
                            );
                        }

                        @Override
                        public void onUnlockB() {

                            if (gerenciadorEstadoControles != null) {

                                gerenciadorEstadoControles.liberarUsuarioB();
                            }

                            atualizarVisibilidadeControles();

                            Log.d(
                                    TAG,
                                    "SERVIDOR LIBEROU OS CONTROLES DO USUÁRIO B."
                            );

                            status.setText(
                                    "Controles liberados!"
                            );
                        }

                        @Override
                        public void onError(String mensagem) {

                            status.setText(
                                    "Erro do servidor: " + mensagem
                            );
                        }

                        @Override
                        public void onControl(JSONObject dados) {

                            processarControleRemoto(dados);
                        }

                        @Override
                        public void onMensagemDesconhecida(String tipo) {

                            Log.d(
                                    TAG,
                                    "Mensagem não reconhecida: " + tipo
                            );
                        }
                    });

    // =====================================================
    // ESTADO DA ATIVIDADE
    // =====================================================

    private boolean atividadeEncerrada = false;

    private boolean modoIndividual = false;

    private boolean controleRemotoAtivo = false;

    private boolean carregando = false;

    // =====================================================
    // GERENCIADOR DE NAVEGAÇÃO DOS GIFS
    // =====================================================

    private final GerenciadorNavegacaoGif gerenciadorNavegacaoGif =
            new GerenciadorNavegacaoGif();

    private long geracaoExibicao = 0;

    // =====================================================
    // GERENCIADOR DE MOVIMENTAÇÃO
    // =====================================================

    private final GerenciadorMovimento gerenciadorMovimento =
            new GerenciadorMovimento(
                    new GerenciadorMovimento.Listener() {

                        @Override
                        public boolean possuiGif() {

                            return movie != null;
                        }

                        @Override
                        public void avancarParaProximoGif() {

                            proximoGif();
                        }

                        @Override
                        public void retomarAnimacao() {

                            iniciarAnimacao();
                        }
                    }
            );

    // =====================================================
    // GERENCIADOR MULTITOQUE
    // =====================================================

    private GerenciadorControleMultitoque gerenciadorControle;

    // =====================================================
    // GERENCIADOR DE CONTROLE REMOTO
    // =====================================================

    private GerenciadorControleRemoto gerenciadorControleRemoto;

    // =====================================================
    // GERENCIADOR DE REPRODUÇÃO
    // =====================================================

    private GerenciadorReproducaoGif gerenciadorReproducao;

    // =====================================================
    // GERENCIADOR DO TEXTO DA PLANILHA
    // =====================================================

    private GerenciadorTextoPlanilha gerenciadorTextoPlanilha;

    // =====================================================
    // ANIMAÇÃO E RENDERIZAÇÃO
    // =====================================================

    private Movie movie;

    private GifFrameRenderer renderizadorGif;

    private final Paint pincel = new Paint(
            Paint.ANTI_ALIAS_FLAG |
                    Paint.FILTER_BITMAP_FLAG
    );

    // =====================================================
    // VERIFICAÇÃO DO CICLO DE VIDA
    // =====================================================

    private boolean atividadeDisponivel() {

        return !atividadeEncerrada &&
                !isFinishing() &&
                !isDestroyed();
    }

    // =====================================================
    // VISIBILIDADE DOS CONTROLES
    // =====================================================

    private void atualizarVisibilidadeControles() {

        if (gerenciadorEstadoControles == null) {
            return;
        }

        gerenciadorEstadoControles.atualizar(
                servicoConexao
        );
    }

    // =====================================================
    // MODO IMERSIVO
    // =====================================================

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && atividadeDisponivel()) {

            ModoImersivo.ativar(getWindow());
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

            Log.e(
                    TAG,
                    "ServicoConexao não foi inicializado."
            );

            atualizarVisibilidadeControles();

            return;
        }

        if (!servicoConexao.estaConectado()) {

            status.setText(
                    "ERRO: conexão com o servidor foi perdida."
            );

            Log.e(
                    TAG,
                    "A conexão compartilhada não está ativa."
            );

            atualizarVisibilidadeControles();

            return;
        }

        gerenciadorComunicacaoServidor.registrarCallbacks(
                servicoConexao
        );

        Log.d(
                TAG,
                "MainActivity reutilizando a conexão existente."
        );

        status.setText(
                "Conectado. Aguardando comandos..."
        );

        atualizarVisibilidadeControles();
    }

    // =====================================================
    // CONTROLE RECEBIDO DO PARCEIRO
    // =====================================================

    private void processarControleRemoto(JSONObject dados) {

        if (!atividadeDisponivel() ||
                gerenciadorControleRemoto == null) {

            return;
        }

        gerenciadorControleRemoto.processar(dados);
    }

    // =====================================================
    // APLICAR CONTROLE REMOTO
    // =====================================================

    private void aplicarControleRemoto(float progressoRecebido) {

        if (!atividadeDisponivel() ||
                movie == null ||
                gerenciadorReproducao == null ||
                gerenciadorControle == null) {

            return;
        }

        controleRemotoAtivo = true;

        pausarAnimacao();

        gerenciadorMovimento.registrarProgresso(
                progressoRecebido,
                gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );

        int duracao = obterDuracaoGif();

        gerenciadorReproducao.definirPosicao(
                (long) (progressoRecebido * duracao)
        );

        areaGif.post(() -> {

            if (!atividadeDisponivel() ||
                    movie == null ||
                    gerenciadorControle == null) {

                return;
            }

            gerenciadorControle.moverControles(
                    progressoRecebido
            );

            desenharQuadro();
        });
    }

    // =====================================================
    // LIBERAÇÃO DO CONTROLE REMOTO
    // =====================================================

    private void liberarControleRemoto() {

        controleRemotoAtivo = false;

        Log.d(
                TAG,
                "Parceiro liberou os controles."
        );

        finalizarMovimentoSeLiberado();
    }

    // =====================================================
    // ENVIO DE COMANDOS AO PARCEIRO
    // =====================================================

    private void enviarComandoControle(
            boolean ativo,
            float progresso) {

        if (gerenciadorControleRemoto == null) {
            return;
        }

        gerenciadorControleRemoto.enviar(
                servicoConexao,
                modoIndividual,
                ativo,
                progresso
        );
    }

    // =====================================================
    // DURAÇÃO DO GIF
    // =====================================================

    private int obterDuracaoGif() {

        if (gerenciadorReproducao == null) {
            return 1000;
        }

        return gerenciadorReproducao.obterDuracao(movie);
    }

    // =====================================================
    // DECISÃO APÓS SOLTAR TODOS OS CONTROLES
    // =====================================================

    private void finalizarMovimentoSeLiberado() {

        if (gerenciadorControle == null) {
            return;
        }

        gerenciadorMovimento.finalizarMovimentoSeLiberado(
                gerenciadorControle.isEsquerdoPressionado(),
                gerenciadorControle.isDireitoPressionado(),
                gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );
    }

    // =====================================================
    // ONCREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ModoImersivo.ativar(getWindow());

        // -------------------------------------------------
        // COMPONENTES VISUAIS
        // -------------------------------------------------

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

        // -------------------------------------------------
        // GERENCIADOR DE COMUNICAÇÃO COM O SERVIDOR
        // -------------------------------------------------

        gerenciadorComunicacaoServidor =
                new GerenciadorComunicacaoServidor(
                        gerenciadorMensagens,
                        new GerenciadorComunicacaoServidor.Listener() {

                            @Override
                            public boolean atividadeDisponivel() {

                                return MainActivity.this.atividadeDisponivel();
                            }

                            @Override
                            public void onConectado() {

                                Log.d(
                                        TAG,
                                        "Conexão compartilhada ativa."
                                );

                                status.setText(
                                        "Conectado ao servidor."
                                );

                                atualizarVisibilidadeControles();
                            }

                            @Override
                            public void onDesconectado(String motivo) {

                                Log.w(
                                        TAG,
                                        "Servidor desconectado: " + motivo
                                );

                                controleRemotoAtivo = false;

                                finalizarMovimentoSeLiberado();

                                status.setText(
                                        "Servidor desconectado."
                                );
                            }

                            @Override
                            public void onErroConexao(String erro) {

                                Log.e(
                                        TAG,
                                        "Erro de conexão: " + erro
                                );

                                status.setText(
                                        "Erro de conexão: " + erro
                                );
                            }
                        }
                );

        // -------------------------------------------------
        // MODO DE EXECUÇÃO
        // -------------------------------------------------

        modoIndividual = getIntent().getBooleanExtra(
                "modo_individual",
                false
        );

        // -------------------------------------------------
        // GERENCIADOR DE ESTADO DOS CONTROLES
        // -------------------------------------------------

        gerenciadorEstadoControles =
                new GerenciadorEstadoControles(
                        controleEsquerdo,
                        controleDireito
                );

        gerenciadorEstadoControles.definirModoIndividual(
                modoIndividual
        );

        // -------------------------------------------------
        // RENDERIZADOR
        // -------------------------------------------------

        renderizadorGif = new GifFrameRenderer(imagemGif);

        // -------------------------------------------------
        // GERENCIADOR DE DECODIFICAÇÃO DOS GIFS
        // -------------------------------------------------

        gerenciadorDecodificacaoGif =
                new GerenciadorDecodificacaoGif(
                        executor,
                        handler
                );

        // -------------------------------------------------
        // REPOSITÓRIO DE GIFS
        // -------------------------------------------------

        gifRepository = new GifRepository(
                new File(
                        getFilesDir(),
                        "gifs"
                )
        );

        // -------------------------------------------------
        // GERENCIADOR DE CARREGAMENTO DOS GIFS
        // -------------------------------------------------

        gerenciadorCarregamentoGif =
                new GerenciadorCarregamentoGif(
                        gifRepository,
                        executor,
                        handler,
                        new GerenciadorCarregamentoGif.Listener() {

                            @Override
                            public void onCarregamentoIniciado() {

                                if (!atividadeDisponivel()) {
                                    return;
                                }

                                carregando = true;

                                botaoProximo.setEnabled(false);

                                status.setText(
                                        "Carregando: " +
                                                nomeGifSolicitado
                                );
                            }

                            @Override
                            public void onCarregamentoConcluido(
                                    File arquivo) {

                                if (!atividadeDisponivel()) {
                                    return;
                                }

                                carregando = false;

                                botaoProximo.setEnabled(true);

                                exibirGif(
                                        arquivo,
                                        nomeGifSolicitado
                                );
                            }

                            @Override
                            public void onErro(Exception erro) {

                                if (!atividadeDisponivel()) {
                                    return;
                                }

                                carregando = false;

                                botaoProximo.setEnabled(true);

                                Log.e(
                                        TAG,
                                        "Erro ao carregar GIF.",
                                        erro
                                );

                                status.setText(
                                        "Erro ao obter GIF: " +
                                                erro.getMessage()
                                );
                            }
                        }
                );

        // -------------------------------------------------
        // GERENCIADOR DE REPRODUÇÃO
        // -------------------------------------------------

        gerenciadorReproducao =
                new GerenciadorReproducaoGif(
                        handler,
                        INTERVALO_ANIMACAO_MS,
                        new GerenciadorReproducaoGif.Listener() {

                            @Override
                            public boolean podeIniciar() {

                                return atividadeDisponivel() &&
                                        movie != null &&
                                        (gerenciadorControle == null ||
                                                !gerenciadorControle.isControleManualAtivo()) &&
                                        !controleRemotoAtivo;
                            }

                            @Override
                            public void atualizarQuadro() {

                                desenharQuadro();
                            }
                        }
                );

        // -------------------------------------------------
        // GERENCIADOR DO TEXTO DA PLANILHA
        // -------------------------------------------------

        gerenciadorTextoPlanilha =
                new GerenciadorTextoPlanilha(
                        textoPlanilha,
                        areaGif,
                        handler,
                        new GerenciadorTextoPlanilha.Listener() {

                            @Override
                            public boolean atividadeDisponivel() {

                                return MainActivity.this.atividadeDisponivel();
                            }

                            @Override
                            public boolean possuiGif() {

                                return movie != null;
                            }
                        }
                );

        gerenciadorTextoPlanilha.configurar();

        // -------------------------------------------------
        // GERENCIADOR MULTITOQUE
        // -------------------------------------------------

        gerenciadorControle =
                new GerenciadorControleMultitoque(
                        areaGif,
                        controleEsquerdo,
                        controleDireito,
                        new GerenciadorControleMultitoque.Listener() {

                            @Override
                            public boolean possuiGif() {

                                return movie != null;
                            }

                            @Override
                            public void onControleManualIniciado() {

                                pausarAnimacao();
                            }

                            @Override
                            public void onProgressoManual(float progresso) {

                                gerenciadorMovimento.registrarProgresso(
                                        progresso,
                                        true,
                                        controleRemotoAtivo
                                );

                                int duracao = obterDuracaoGif();

                                gerenciadorReproducao.definirPosicao(
                                        (long) (progresso * duracao)
                                );

                                desenharQuadro();

                                enviarComandoControle(
                                        true,
                                        progresso
                                );
                            }

                            @Override
                            public void onControleManualLiberado() {

                                enviarComandoControle(
                                        false,
                                        0f
                                );

                                finalizarMovimentoSeLiberado();
                            }
                        }
                );

        // -------------------------------------------------
        // GERENCIADOR DE CONTROLE REMOTO
        // -------------------------------------------------

        gerenciadorControleRemoto =
                new GerenciadorControleRemoto(
                        new GerenciadorControleRemoto.Listener() {

                            @Override
                            public void onControleRecebido(float progresso) {

                                aplicarControleRemoto(progresso);
                            }

                            @Override
                            public void onControleLiberado() {

                                liberarControleRemoto();
                            }
                        }
                );

        // -------------------------------------------------
        // BOTÃO PRÓXIMO GIF
        // -------------------------------------------------

        botaoProximo.setOnClickListener(v -> {

            gerenciadorMovimento.reiniciarContagem();

            proximoGif();
        });

        configurarControles();

        // -------------------------------------------------
        // CONFIGURAÇÃO DA CONEXÃO
        // -------------------------------------------------

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

        atualizarVisibilidadeControles();

        // -------------------------------------------------
        // GERENCIADOR DE CARREGAMENTO DA PLANILHA
        // -------------------------------------------------

        gerenciadorCarregamentoPlanilha =
                new GerenciadorCarregamentoPlanilha(
                        planilhaRepository,
                        executor,
                        handler,
                        new GerenciadorCarregamentoPlanilha.Listener() {

                            @Override
                            public void onCarregamentoConcluido(
                                    GerenciadorCarregamentoPlanilha.Resultado resultado) {

                                if (!atividadeDisponivel()) {
                                    return;
                                }

                                nomes.clear();
                                links.clear();
                                textosPlanilha.clear();

                                nomes.addAll(resultado.getNomes());
                                links.addAll(resultado.getLinks());
                                textosPlanilha.addAll(resultado.getTextos());

                                gerenciadorNavegacaoGif.reiniciar();

                                if (resultado.estaVazio()) {

                                    status.setText(
                                            "Nenhum GIF encontrado."
                                    );

                                    botaoProximo.setEnabled(false);

                                } else {

                                    status.setText(
                                            resultado.getQuantidade() +
                                                    " GIFs disponíveis."
                                    );

                                    botaoProximo.setEnabled(true);

                                    proximoGif();
                                }
                            }

                            @Override
                            public void onErro(Exception erro) {

                                if (!atividadeDisponivel()) {
                                    return;
                                }

                                status.setText(
                                        "Erro ao consultar planilha: " +
                                                erro.getMessage()
                                );

                                botaoProximo.setEnabled(false);

                                Log.e(
                                        TAG,
                                        "Falha no carregamento da planilha.",
                                        erro
                                );
                            }
                        }
                );

        gerenciadorCarregamentoPlanilha.carregar();
    }

    // =====================================================
    // CONFIGURAÇÃO DOS CONTROLES
    // =====================================================

    private void configurarControles() {

        gerenciadorControle.configurar();
    }

    // =====================================================
    // PAUSAR ANIMAÇÃO
    // =====================================================

    private void pausarAnimacao() {

        if (gerenciadorReproducao == null) {
            return;
        }

        gerenciadorReproducao.pausar(movie);
    }

    // =====================================================
    // RETOMAR ANIMAÇÃO
    // =====================================================

    private void iniciarAnimacao() {

        if (gerenciadorReproducao == null) {
            return;
        }

        gerenciadorReproducao.iniciar(movie);
    }

    // =====================================================
    // DESENHO DO QUADRO
    // =====================================================

    private void desenharQuadro() {

        if (!atividadeDisponivel() ||
                movie == null ||
                renderizadorGif == null ||
                gerenciadorReproducao == null) {

            return;
        }

        renderizadorGif.desenhar(
                movie,
                gerenciadorReproducao.getPosicaoMs(),
                pincel
        );
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

        int indiceSelecionado =
                gerenciadorNavegacaoGif.proximo(nomes.size());

        if (indiceSelecionado < 0) {
            return;
        }

        baixarEExibir(indiceSelecionado);
    }

    // =====================================================
    // SOLICITAÇÃO DE CARREGAMENTO DO GIF
    // =====================================================

    private void baixarEExibir(int posicao) {

        if (!atividadeDisponivel() || carregando) {
            return;
        }

        if (posicao < 0 ||
                posicao >= nomes.size() ||
                posicao >= links.size()) {

            Log.e(
                    TAG,
                    "Posição inválida para carregamento do GIF: " +
                            posicao
            );

            return;
        }

        nomeGifSolicitado = nomes.get(posicao);

        String link = links.get(posicao);

        status.setText(
                "Carregando: " + nomeGifSolicitado
        );

        gerenciadorCarregamentoGif.carregar(link);
    }

    // =====================================================
    // EXIBIÇÃO DO GIF PRINCIPAL
    // =====================================================

    private void exibirGif(
            File arquivo,
            String nome) {

        if (!atividadeDisponivel()) {
            return;
        }

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() == 0) {

            status.setText(
                    "Arquivo GIF inválido."
            );

            return;
        }

        final long geracaoAtual = ++geracaoExibicao;

        // -------------------------------------------------
        // INTERROMPER GIF ANTERIOR
        // -------------------------------------------------

        gerenciadorReproducao.reiniciar();

        gerenciadorTextoPlanilha.parar();

        if (gerenciadorControle != null &&
                gerenciadorControle.isControleManualAtivo()) {

            enviarComandoControle(false, 0f);
        }

        movie = null;

        controleRemotoAtivo = false;

        // -------------------------------------------------
        // REINICIAR CONTROLES E MOVIMENTO
        // -------------------------------------------------

        gerenciadorMovimento.reiniciarContagem();

        gerenciadorControle.limpar();

        atualizarVisibilidadeControles();

        areaGif.post(() -> {

            if (!atividadeDisponivel() ||
                    geracaoAtual != geracaoExibicao) {

                return;
            }

            gerenciadorControle.moverControles(0.5f);
        });

        // -------------------------------------------------
        // DECODIFICAR O GIF
        // -------------------------------------------------

        gerenciadorDecodificacaoGif.decodificar(
                arquivo,
                new GerenciadorDecodificacaoGif.Listener() {

                    @Override
                    public void onDecodificacaoConcluida(
                            Movie resultado) {

                        if (!atividadeDisponivel() ||
                                geracaoAtual != geracaoExibicao) {

                            return;
                        }

                        movie = resultado;

                        gerenciadorReproducao.reiniciar();

                        // ---------------------------------
                        // TEXTO ASSOCIADO AO GIF
                        // ---------------------------------

                        final String textoFinal;

                        int indiceAtual =
                                gerenciadorNavegacaoGif.getIndiceAtual();

                        if (indiceAtual >= 0 &&
                                indiceAtual < textosPlanilha.size()) {

                            textoFinal =
                                    textosPlanilha.get(indiceAtual);

                        } else {

                            textoFinal = "";
                        }

                        final int indiceExibido =
                                indiceAtual + 1;

                        areaGif.post(() -> {

                            if (!atividadeDisponivel() ||
                                    geracaoAtual != geracaoExibicao) {

                                return;
                            }

                            desenharQuadro();

                            iniciarAnimacao();

                            gerenciadorTextoPlanilha.iniciar(
                                    textoFinal
                            );

                            status.setText(
                                    nome + " | " +
                                            indiceExibido +
                                            "/" + nomes.size()
                            );
                        });
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (!atividadeDisponivel() ||
                                geracaoAtual != geracaoExibicao) {

                            return;
                        }

                        gerenciadorReproducao.reiniciar();

                        gerenciadorTextoPlanilha.parar();

                        movie = null;

                        if (arquivo.exists()) {
                            arquivo.delete();
                        }

                        status.setText(
                                "Erro ao abrir GIF: " +
                                        erro.getMessage()
                        );

                        Log.e(
                                TAG,
                                "Erro ao decodificar GIF.",
                                erro
                        );
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

        geracaoExibicao++;

        // -------------------------------------------------
        // ENCERRAR GERENCIADOR DE REPRODUÇÃO
        // -------------------------------------------------

        if (gerenciadorReproducao != null) {

            gerenciadorReproducao.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR GERENCIADOR DO TEXTO
        // -------------------------------------------------

        if (gerenciadorTextoPlanilha != null) {

            gerenciadorTextoPlanilha.encerrar();
        }

        // -------------------------------------------------
        // REMOVER CALLBACKS PENDENTES
        // -------------------------------------------------

        handler.removeCallbacksAndMessages(null);

        // -------------------------------------------------
        // LIMPAR ESTADO
        // -------------------------------------------------

        movie = null;

        controleRemotoAtivo = false;

        carregando = false;

        nomeGifSolicitado = "";

        gerenciadorMovimento.reiniciarContagem();

        if (gerenciadorControle != null) {

            gerenciadorControle.limpar();
        }

        // -------------------------------------------------
        // REMOVER CALLBACK DA COMUNICAÇÃO
        // -------------------------------------------------

        /*
         * O GerenciadorComunicacaoServidor remove somente
         * o callback registrado pela MainActivity.
         *
         * A conexão WebSocket continua pertencendo
         * à ConexaoActivity.
         */

        if (gerenciadorComunicacaoServidor != null) {

            gerenciadorComunicacaoServidor.removerCallback();
        }

        // -------------------------------------------------
        // LIBERAR RENDERIZADOR
        // -------------------------------------------------

        if (renderizadorGif != null) {

            renderizadorGif.liberar();

            renderizadorGif = null;
        }

        // -------------------------------------------------
        // ENCERRAR EXECUTOR
        // -------------------------------------------------

        executor.shutdownNow();

        super.onDestroy();
    }
}