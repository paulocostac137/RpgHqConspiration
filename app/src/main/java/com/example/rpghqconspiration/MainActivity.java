package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.graphics.drawable.GradientDrawable;

import android.util.Log;

import android.view.View;
import android.view.ViewGroup;

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

    private static final long INTERVALO_ANIMACAO_MS = 33;
    private static final long INTERVALO_TROCA_GIF_MS = 10000;
    private static final long INTERVALO_EXIBICAO_TEXTO_MS = 5000;
    private static final long ANTECEDENCIA_LIBERACAO_CONTROLES_MS = 2000;
    private static final int AMOSTRAS_RELOGIO_SERVIDOR = 3;
    private static final long ATRASO_AMOSTRA_RELOGIO_MS = 100;

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

    private final ArrayList<String> nomes = new ArrayList<>();
    private final ArrayList<String> links = new ArrayList<>();
    private final ArrayList<String> textosPlanilha = new ArrayList<>();
    private final ArrayList<Integer> repeticoesCiclos = new ArrayList<>();

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
    // GERENCIADOR DE DECODIFICAÇÃO DOS GIFS
    // =====================================================

    private GerenciadorDecodificacaoGif
            gerenciadorDecodificacaoGif;

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

                            salaComParceiro = usuarios >= 2;
                            if (salaComParceiro) {
                                iniciarSincronizacaoRelogio();
                                solicitarSincronizacaoInicialSePronto();
                            }

                            atualizarVisibilidadeControles();

                            status.setText(
                                    usuarios == 1
                                            ? "Conectado. Aguardando parceiro..."
                                            : "Conectado! Dois usuários na sala."
                            );
                        }

                        @Override
                        public void onPeerConnected() {

                            salaComParceiro = true;
                            sincronizacaoInicialSolicitada = false;
                            iniciarSincronizacaoRelogio();

                            status.setText(
                                    "PARCEIRO CONECTADO!"
                            );

                            solicitarSincronizacaoInicialSePronto();

                            Log.d(
                                    TAG,
                                    "O parceiro entrou na sala."
                            );
                        }

                        @Override
                        public void onPeerDisconnected() {

                            salaComParceiro = false;
                            sincronizacaoInicialSolicitada = false;
                            aguardandoInicioSincronizado = false;
                            gifSincronizadoPronto = false;
                            handler.removeCallbacks(iniciarGifSincronizado);
                            if (gifTiming != null) {
                                gifTiming.pararTrocaAutomatica();
                            }
                            pausarAnimacao();

                            controleRemotoAtivo = false;
                            gerenciadorMovimento.reiniciarContagem();

                            status.setText(
                                    "Parceiro desconectado."
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
                        public void onSync(JSONObject dados) {

                            processarMensagemSincronizacao(dados);
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

    private String papelUsuarioLocal = "A";
    private String papelControleAtual = "A";
    private boolean controlesPassivosVisiveis = false;
    private Boolean estiloControlesPassivos = null;

    private boolean controleRemotoAtivo = false;
    private float progressoControlesAtual = 0.5f;

    private boolean carregando = false;
    private boolean planilhaCarregada = false;
    private boolean salaComParceiro = false;
    private boolean sincronizacaoInicialSolicitada = false;
    private boolean aguardandoInicioSincronizado = false;
    private boolean gifSincronizadoPronto = false;
    private int revisaoSincronizacao = -1;
    private int indiceGifSincronizado = -1;
    private String idGifSincronizado = "";
    private int amostrasRelogioSolicitadas = 0;
    private long menorRoundTripMs = Long.MAX_VALUE;
    private long diferencaRelogioServidorMs = 0L;
    private long inicioGifServidorMs = 0L;

    private final Runnable iniciarGifSincronizado = () ->
            iniciarGifSincronizadoAgora();

    // =====================================================
    // GERENCIADOR DE NAVEGAÇÃO DOS GIFS
    // =====================================================

    private final GerenciadorNavegacaoGif gerenciadorNavegacaoGif =
            new GerenciadorNavegacaoGif();

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

    private final Runnable mostrarControlesPassivos = () -> {
        if (!atividadeDisponivel() || ehDonoDoControle()) {
            return;
        }

        controlesPassivosVisiveis = true;
        atualizarPermissaoControles();
        Log.d(TAG, "Controles passivos exibidos para solicitar a troca.");
    };

    // =====================================================
    // GERENCIADOR DE CONTROLE REMOTO
    // =====================================================

    private GerenciadorControleRemoto gerenciadorControleRemoto;

    // =====================================================
    // GERENCIADOR DE REPRODUÇÃO
    // =====================================================

    private GerenciadorReproducaoGif gerenciadorReproducao;

    private GifTiming gifTiming;

    // =====================================================
    // GERENCIADOR DO TEXTO DA PLANILHA
    // =====================================================

    private GerenciadorTextoPlanilha gerenciadorTextoPlanilha;

    // =====================================================
    // ANIMAÇÃO E RENDERIZAÇÃO
    // =====================================================

    private GifFrameSequence movie;
    private long geracaoLinhaDoTempo = 0;
    private int indiceInicialLinhaDoTempo = 0;
    private int indiceSegmentoExibido = -1;
    private boolean preparandoLinhaDoTempo = false;

    private GifFrameRenderer renderizadorGif;

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

        if (gerenciadorEstadoControles != null) {
            gerenciadorEstadoControles.atualizar(
                    servicoConexao
            );
        }

        atualizarPermissaoControles();
    }

    private void atualizarPermissaoControles() {

        if (controleEsquerdo == null || controleDireito == null) {
            return;
        }

        boolean conexaoDisponivel = modoIndividual ||
                (servicoConexao != null &&
                        servicoConexao.estaConectado());
        boolean ehDono = modoIndividual || ehDonoDoControle();
        boolean habilitado = conexaoDisponivel &&
                (ehDono || controlesPassivosVisiveis);

        if (gerenciadorControle != null) {
            gerenciadorControle.definirHabilitado(habilitado);
            gerenciadorControle.definirModoSolicitacao(!ehDono);
        }

        aplicarEstiloControles(!ehDono);
        boolean mostrar = conexaoDisponivel &&
                (ehDono || controlesPassivosVisiveis);
        int visibilidade = mostrar ? View.VISIBLE : View.GONE;
        controleEsquerdo.setVisibility(visibilidade);
        controleDireito.setVisibility(visibilidade);
    }

    private boolean papelEhUsuarioB(String papel) {
        return papel != null && "B".equalsIgnoreCase(papel.trim());
    }

    private boolean ehDonoDoControle() {
        return papelUsuarioLocal.equals(papelControleAtual);
    }

    private int dp(float valor) {
        return Math.round(
                valor * getResources().getDisplayMetrics().density
        );
    }

    private void aplicarEstiloControles(boolean passivos) {
        if (estiloControlesPassivos != null &&
                estiloControlesPassivos == passivos) {
            return;
        }

        estiloControlesPassivos = passivos;
        int largura = dp(passivos ? 56 : 48);
        int altura = dp(passivos ? 56 : 100);
        float raio = dp(passivos ? 28 : 8);

        configurarEstiloControle(controleEsquerdo, largura, altura, raio,
                passivos);
        configurarEstiloControle(controleDireito, largura, altura, raio,
                passivos);

        if (!passivos && gerenciadorControle != null) {
            gerenciadorControle.moverControles(.5f);
        }
    }

    private void configurarEstiloControle(
            TextView controle,
            int largura,
            int altura,
            float raio,
            boolean passivo) {
        FrameLayout.LayoutParams parametros =
                (FrameLayout.LayoutParams) controle.getLayoutParams();
        parametros.width = largura;
        parametros.height = altura;
        parametros.gravity = passivo
                ? android.view.Gravity.CENTER
                : android.view.Gravity.TOP |
                        android.view.Gravity.START;
        parametros.topMargin = 0;
        parametros.bottomMargin = 0;
        parametros.leftMargin = 0;
        parametros.rightMargin = 0;
        controle.setLayoutParams(parametros);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(0xCC222222);
        if (passivo) {
            fundo.setShape(GradientDrawable.OVAL);
        } else {
            fundo.setCornerRadius(raio);
        }
        controle.setBackground(fundo);
    }

    private void aplicarTurnoControle(String papel) {
        if (!"A".equals(papel) && !"B".equals(papel)) {
            return;
        }

        if (papelControleAtual.equals(papel)) {
            return;
        }

        handler.removeCallbacks(mostrarControlesPassivos);
        papelControleAtual = papel;
        controlesPassivosVisiveis = false;
        controleRemotoAtivo = false;

        if (gerenciadorControle != null &&
                !gerenciadorControle.isAguardandoFimSolicitacao()) {
            gerenciadorControle.limpar();
        }

        atualizarPermissaoControles();

        if (gifTiming != null && movie != null) {
            gifTiming.iniciarTrocaAutomatica();
        }

        if (movie != null) {
            iniciarAnimacao();
        }

        if (!ehDonoDoControle() && movie != null) {
            handler.postDelayed(
                    mostrarControlesPassivos,
                    INTERVALO_TROCA_GIF_MS -
                            ANTECEDENCIA_LIBERACAO_CONTROLES_MS
            );
        }
    }

    private void solicitarTrocaDeControle() {
        if (modoIndividual || ehDonoDoControle() ||
                servicoConexao == null ||
                !servicoConexao.estaConectado()) {
            if (gifTiming != null) {
                gifTiming.retomarTrocaAutomatica();
            }
            return;
        }

        String novoDono = papelUsuarioLocal;
        if (!servicoConexao.enviarTrocaControle(novoDono)) {
            if (gifTiming != null) {
                gifTiming.retomarTrocaAutomatica();
            }
            return;
        }
        aplicarTurnoControle(novoDono);
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
        salaComParceiro = servicoConexao.estaSalaCompleta();
        iniciarSincronizacaoRelogio();

        String papelUsuario = servicoConexao.getPapelUsuario();
        if ("A".equalsIgnoreCase(papelUsuario.trim()) ||
                papelEhUsuarioB(papelUsuario)) {
            papelUsuarioLocal = papelUsuario.trim().toUpperCase();
            String turnoServidor =
                    servicoConexao.getPapelControleAtual();
            if ("A".equals(turnoServidor) ||
                    "B".equals(turnoServidor)) {
                papelControleAtual = turnoServidor;
            }
            atualizarPermissaoControles();
        }

        Log.d(
                TAG,
                "MainActivity reutilizando a conexão existente."
        );

        status.setText(
                "Conectado. Aguardando comandos..."
        );

        atualizarVisibilidadeControles();
    }

    private void iniciarSincronizacaoRelogio() {
        if (servicoConexao == null
                || !servicoConexao.estaConectado()
                || amostrasRelogioSolicitadas > 0) {
            return;
        }

        solicitarAmostraRelogio();
    }

    private void solicitarAmostraRelogio() {
        if (servicoConexao == null
                || !servicoConexao.estaConectado()
                || amostrasRelogioSolicitadas >= AMOSTRAS_RELOGIO_SERVIDOR) {
            return;
        }

        long enviadoEm = System.currentTimeMillis();
        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "sync");
            mensagem.put("action", "clock");
            mensagem.put("client_time_ms", enviadoEm);

            if (servicoConexao.enviarMensagemSincronizacao(mensagem)) {
                amostrasRelogioSolicitadas++;
            }
        } catch (Exception erro) {
            Log.e(TAG, "Não foi possível solicitar a hora do servidor.", erro);
        }
    }

    private void processarMensagemSincronizacao(JSONObject dados) {
        if (!atividadeDisponivel() || dados == null) {
            return;
        }

        String acao = dados.optString("action", "");
        if ("clock_reply".equals(acao)) {
            atualizarDiferencaRelogioServidor(dados);
            if (amostrasRelogioSolicitadas < AMOSTRAS_RELOGIO_SERVIDOR) {
                handler.postDelayed(
                        this::solicitarAmostraRelogio,
                        ATRASO_AMOSTRA_RELOGIO_MS
                );
            }
            return;
        }

        if ("prepare".equals(acao)) {
            prepararGifSincronizado(dados);
            return;
        }

        if ("start".equals(acao)) {
            agendarInicioGifSincronizado(dados);
            return;
        }

        if ("error".equals(acao)) {
            aguardandoInicioSincronizado = false;
            gifSincronizadoPronto = false;
            status.setText(
                    "Não foi possível sincronizar o GIF: " +
                            dados.optString("message", "erro desconhecido")
            );
        }
    }

    private void atualizarDiferencaRelogioServidor(JSONObject dados) {
        long enviadoEm = dados.optLong("client_time_ms", -1L);
        long servidorEm = dados.optLong("server_time_ms", -1L);
        long recebidoEm = System.currentTimeMillis();

        if (enviadoEm < 0L || servidorEm < 0L || recebidoEm < enviadoEm) {
            return;
        }

        long roundTrip = recebidoEm - enviadoEm;
        if (roundTrip < menorRoundTripMs) {
            menorRoundTripMs = roundTrip;
            diferencaRelogioServidorMs =
                    servidorEm - (enviadoEm + roundTrip / 2L);
        }
    }

    private void solicitarSincronizacaoInicialSePronto() {
        if (modoIndividual
                || !salaComParceiro
                || !planilhaCarregada
                || sincronizacaoInicialSolicitada) {
            return;
        }

        if (!"A".equals(papelUsuarioLocal)) {
            status.setText("Planilha pronta. Aguardando o usuário A...");
            return;
        }

        if (nomes.isEmpty() || links.isEmpty()) {
            return;
        }

        sincronizacaoInicialSolicitada =
                solicitarPreparacaoGifSincronizado(0);
    }

    private boolean solicitarPreparacaoGifSincronizado(int indice) {
        if (servicoConexao == null
                || !servicoConexao.estaSalaCompleta()
                || indice < 0
                || indice >= links.size()) {
            return false;
        }

        String fileId = GoogleDriveUtils.extrairId(links.get(indice));
        if (fileId == null) {
            status.setText("Link do GIF inválido: " + nomes.get(indice));
            return false;
        }

        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "sync");
            mensagem.put("action", "prepare");
            mensagem.put("index", indice);
            mensagem.put("file_id", fileId);

            boolean enviado =
                    servicoConexao.enviarMensagemSincronizacao(mensagem);
            if (enviado) {
                status.setText(
                        "Preparando GIF para os dois usuários..."
                );
            }
            return enviado;
        } catch (Exception erro) {
            Log.e(TAG, "Não foi possível preparar GIF sincronizado.", erro);
            status.setText("Erro ao preparar sincronização do GIF.");
            return false;
        }
    }

    private int localizarGifPorId(String fileId) {
        for (int i = 0; i < links.size(); i++) {
            if (fileId.equals(GoogleDriveUtils.extrairId(links.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private void prepararGifSincronizado(JSONObject dados) {
        if (modoIndividual || !salaComParceiro) {
            return;
        }

        int revisao = dados.optInt("revision", -1);
        String fileId = dados.optString("file_id", "");
        int indice = localizarGifPorId(fileId);
        if (revisao <= revisaoSincronizacao || indice < 0) {
            if (indice < 0) {
                enviarFalhaPreparacaoSincronizada(
                        revisao,
                        "O GIF sincronizado não existe na planilha local."
                );
            }
            return;
        }

        revisaoSincronizacao = revisao;
        indiceGifSincronizado = indice;
        idGifSincronizado = fileId;
        aguardandoInicioSincronizado = true;
        gifSincronizadoPronto = false;
        carregando = false;

        handler.removeCallbacks(iniciarGifSincronizado);
        gerenciadorReproducao.reiniciar();
        gifTiming.pararTrocaAutomatica();
        gerenciadorTextoPlanilha.parar();
        if (movie == null) {
            baixarEExibir(indice);
        } else {
            prepararExibicaoSincronizada(indice);
        }
    }

    private void enviarProntoParaSincronizacao() {
        if (servicoConexao == null
                || !aguardandoInicioSincronizado
                || gifSincronizadoPronto
                || revisaoSincronizacao < 0) {
            return;
        }

        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "sync");
            mensagem.put("action", "ready");
            mensagem.put("revision", revisaoSincronizacao);
            mensagem.put("index", indiceGifSincronizado);
            mensagem.put("file_id", idGifSincronizado);

            if (servicoConexao.enviarMensagemSincronizacao(mensagem)) {
                gifSincronizadoPronto = true;
                status.setText("GIF pronto. Aguardando o parceiro...");
            }
        } catch (Exception erro) {
            Log.e(TAG, "Não foi possível confirmar GIF pronto.", erro);
        }
    }

    private void enviarFalhaPreparacaoSincronizada(
            int revisao,
            String mensagemErro
    ) {
        if (servicoConexao == null || revisao < 0) {
            return;
        }

        try {
            JSONObject mensagem = new JSONObject();
            mensagem.put("type", "sync");
            mensagem.put("action", "failed");
            mensagem.put("revision", revisao);
            mensagem.put("index", indiceGifSincronizado);
            mensagem.put("file_id", idGifSincronizado);
            mensagem.put("message", mensagemErro);
            servicoConexao.enviarMensagemSincronizacao(mensagem);
        } catch (Exception erro) {
            Log.e(TAG, "Não foi possível informar falha de sincronização.", erro);
        }
    }

    private void agendarInicioGifSincronizado(JSONObject dados) {
        int revisao = dados.optInt("revision", -1);
        int indice = dados.optInt("index", -1);
        if (!gifSincronizadoPronto
                || revisao != revisaoSincronizacao
                || indice != indiceGifSincronizado
                || !idGifSincronizado.equals(
                        dados.optString("file_id", "")
                )) {
            return;
        }

        inicioGifServidorMs =
                dados.optLong("start_at_server_ms", 0L);
        long inicioLocalMs =
                inicioGifServidorMs - diferencaRelogioServidorMs;
        long atraso = Math.max(
                0L,
                inicioLocalMs - System.currentTimeMillis()
        );

        handler.removeCallbacks(iniciarGifSincronizado);
        handler.postDelayed(iniciarGifSincronizado, atraso);
    }

    private void iniciarGifSincronizadoAgora() {
        if (!atividadeDisponivel()
                || !aguardandoInicioSincronizado
                || !gifSincronizadoPronto
                || movie == null) {
            return;
        }

        aguardandoInicioSincronizado = false;
        gifSincronizadoPronto = false;
        gerenciadorReproducao.reiniciar();
        gerenciadorReproducao.definirSequencia(movie);
        gerenciadorReproducao.definirIndiceQuadro(
                movie.getFrameForSegment(indiceGifSincronizado)
        );
        progressoControlesAtual = movie.getProgressForSegment(
                indiceGifSincronizado
        );
        indiceSegmentoExibido = indiceGifSincronizado;
        desenharQuadro();
        iniciarAnimacao();
        if (!controleRemotoAtivo &&
                (gerenciadorControle == null ||
                        !gerenciadorControle.isControleManualAtivo())) {
            gifTiming.iniciarTrocaAutomatica();
        }

        if (!ehDonoDoControle()) {
            handler.removeCallbacks(mostrarControlesPassivos);
            handler.postDelayed(
                    mostrarControlesPassivos,
                    Math.max(
                            0,
                            INTERVALO_TROCA_GIF_MS -
                                    ANTECEDENCIA_LIBERACAO_CONTROLES_MS
                    )
            );
        }

        String texto = indiceGifSincronizado < textosPlanilha.size()
                ? textosPlanilha.get(indiceGifSincronizado)
                : "";
        gerenciadorTextoPlanilha.iniciarComVisibilidadeExterna(texto);
        status.setText(
                nomes.get(indiceGifSincronizado) + " | " +
                        (indiceGifSincronizado + 1) + "/" + nomes.size()
        );
    }

    // =====================================================
    // CONTROLE RECEBIDO DO PARCEIRO
    // =====================================================

    private void processarControleRemoto(JSONObject dados) {

        if (!atividadeDisponivel() ||
                gerenciadorControleRemoto == null) {

            return;
        }

        if (dados.has("turn_pending")) {
            if (gifTiming != null) {
                if (dados.optBoolean("turn_pending")) {
                    gifTiming.pausarTrocaAutomatica();
                } else {
                    gifTiming.retomarTrocaAutomatica();
                }
            }
            return;
        }

        String novoDono = dados.optString("turn", "");
        if (!novoDono.isEmpty()) {
            aplicarTurnoControle(novoDono);
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

        if (gifTiming != null) {
            gifTiming.definirProgressoExibicao(progressoRecebido);
        }

        pausarAnimacao();

        gerenciadorMovimento.registrarProgresso(
                progressoRecebido,
                gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );

        gerenciadorReproducao.definirProgresso(progressoRecebido        );
        progressoControlesAtual = progressoRecebido;

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
            atualizarSegmentoLinhaDoTempo();
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
        if (gifTiming != null) {
            gifTiming.retomarTrocaAutomatica();
        }
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
    // DECISÃO APÓS SOLTAR TODOS OS CONTROLES
    // =====================================================

    private void finalizarMovimentoSeLiberado() {

        if (gerenciadorControle == null) {
            return;
        }

        gerenciadorMovimento.finalizarMovimentoSeLiberado(
                gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );
    }

    // =====================================================
    // ONCREATE
    // =====================================================

    private void configurarAfastamentoControles() {

        View linhaControles = findViewById(R.id.linhaControles);
        ((ViewGroup) linhaControles)
                .setMotionEventSplittingEnabled(false);
        linhaControles.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) -> {
                    float deslocamento = areaGif.getWidth() * 0.07f;
                    float direcaoInicio =
                            view.getLayoutDirection() ==
                                    View.LAYOUT_DIRECTION_RTL
                                    ? -1f
                                    : 1f;

                    controleEsquerdo.setTranslationX(
                            direcaoInicio * deslocamento
                    );
                    controleDireito.setTranslationX(
                            -direcaoInicio * deslocamento
                    );
                }
        );
    }

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

        controleEsquerdo =
                findViewById(R.id.controleEsquerdo);

        controleDireito =
                findViewById(R.id.controleDireito);

        areaGif = findViewById(R.id.areaGif);
        configurarAfastamentoControles();

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

                                salaComParceiro = false;
                                aguardandoInicioSincronizado = false;
                                gifSincronizadoPronto = false;
                                handler.removeCallbacks(iniciarGifSincronizado);
                                controleRemotoAtivo = false;

                                if (gifTiming != null) {
                                    gifTiming.pararTrocaAutomatica();
                                }
                                pausarAnimacao();
                                gerenciadorMovimento.reiniciarContagem();
                                atualizarVisibilidadeControles();

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
        String papelInicial =
                getIntent().getStringExtra("papel_usuario");
        if ("A".equalsIgnoreCase(papelInicial == null
                ? "" : papelInicial.trim())) {
            papelUsuarioLocal = "A";
        } else if (papelEhUsuarioB(papelInicial)) {
            papelUsuarioLocal = "B";
        }
        String turnoInicial =
                getIntent().getStringExtra("turno_controle");
        if ("A".equals(turnoInicial) || "B".equals(turnoInicial)) {
            papelControleAtual = turnoInicial;
        }

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
                                atualizarSegmentoLinhaDoTempo();
                            }
                        }
                );

        gifTiming = new GifTiming(
                handler,
                INTERVALO_TROCA_GIF_MS,
                INTERVALO_EXIBICAO_TEXTO_MS,
                INTERVALO_ANIMACAO_MS,
                new GifTiming.Callback() {

                    @Override
                    public void onNextGifPreparationRequested() {
                        // Todos os quadros já pertencem à linha do tempo.
                    }

                    @Override
                    public void onAutoAdvanceRequested() {
                        // A reprodução percorre a linha do tempo sem trocar Views.
                    }

                    @Override
                    public void onTextVisibilityChanged(boolean visible) {
                        if (gerenciadorTextoPlanilha != null) {
                            gerenciadorTextoPlanilha.definirVisivel(visible);
                        }
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
                                if (gifTiming != null) {
                                    gifTiming.pausarTrocaAutomatica();
                                }
                            }

                            @Override
                            public void onProgressoManual(float progresso) {

                                if (gifTiming != null) {
                                    gifTiming.definirProgressoExibicao(
                                            progresso
                                    );
                                }

                                gerenciadorMovimento.registrarProgresso(
                                        progresso,
                                        true,
                                        controleRemotoAtivo
                                );

                                progressoControlesAtual = progresso;
                                gerenciadorReproducao.definirProgresso(
                                        progresso
                                );

                                desenharQuadro();
                                atualizarSegmentoLinhaDoTempo();

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
                                if (gifTiming != null) {
                                    gifTiming.retomarTrocaAutomatica();
                                }
                            }

                            @Override
                            public void onSolicitacaoTrocaIniciada() {
                                if (gifTiming != null) {
                                    gifTiming.pausarTrocaAutomatica();
                                }
                                if (servicoConexao != null) {
                                    servicoConexao
                                            .enviarEstadoSolicitacaoTroca(true);
                                }
                            }

                            @Override
                            public void onSolicitacaoTrocaCancelada() {
                                if (gifTiming != null) {
                                    gifTiming.retomarTrocaAutomatica();
                                }
                                if (servicoConexao != null) {
                                    servicoConexao
                                            .enviarEstadoSolicitacaoTroca(false);
                                }
                            }

                            @Override
                            public void onSolicitacaoTrocaConcluida() {
                                solicitarTrocaDeControle();
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

        configurarControles();
        atualizarPermissaoControles();

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
                                repeticoesCiclos.clear();

                                nomes.addAll(resultado.getNomes());
                                links.addAll(resultado.getLinks());
                                textosPlanilha.addAll(resultado.getTextos());
                                repeticoesCiclos.addAll(
                                        resultado.getRepeticoes()
                                );
                                planilhaCarregada = true;

                                gerenciadorNavegacaoGif.reiniciar();

                                if (resultado.estaVazio()) {

                                    status.setText(
                                            "Nenhum GIF encontrado."
                                    );

                                } else {

                                    status.setText(
                                            resultado.getQuantidade() +
                                                    " GIFs disponíveis."
                                    );

                                    if (modoIndividual) {
                                        proximoGif();
                                    } else {
                                        solicitarSincronizacaoInicialSePronto();
                                        if (!salaComParceiro) {
                                            status.setText(
                                                    "Planilha pronta. Aguardando o parceiro..."
                                            );
                                        }
                                    }
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

        gerenciadorReproducao.pausar();
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

    private float limitarProgresso(float progresso) {
        return Math.max(0f, Math.min(1f, progresso));
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
                gerenciadorReproducao.getIndiceQuadro()
        );
    }

    private void atualizarSegmentoLinhaDoTempo() {
        if (movie == null || gerenciadorReproducao == null) {
            return;
        }

        int segmento = movie.getSegmentIndex(
                gerenciadorReproducao.getIndiceQuadro()
        );
        if (segmento == indiceSegmentoExibido ||
                segmento < 0 ||
                segmento >= nomes.size()) {
            return;
        }

        indiceSegmentoExibido = segmento;
        gerenciadorNavegacaoGif.selecionar(segmento, nomes.size());
        String texto = segmento < textosPlanilha.size()
                ? textosPlanilha.get(segmento)
                : "";
        gerenciadorTextoPlanilha.iniciarComVisibilidadeExterna(texto);
        status.setText(
                nomes.get(segmento) + " | " +
                        (segmento + 1) + "/" + nomes.size()
        );
        if (!controleRemotoAtivo &&
                (gerenciadorControle == null ||
                        !gerenciadorControle.isControleManualAtivo())) {
            gifTiming.iniciarTrocaAutomatica();
        }
    }

    // =====================================================
    // NAVEGAÇÃO PARA O PRÓXIMO GIF
    // =====================================================

    private void proximoGif() {

        if (!atividadeDisponivel() || carregando) {
            return;
        }

        if (!modoIndividual) {
            if (!salaComParceiro) {
                status.setText("Aguardando o parceiro para sincronizar os GIFs...");
                return;
            }

            if (nomes.isEmpty()) {
                status.setText("Nenhum GIF disponível.");
                return;
            }

            if (!"A".equals(papelUsuarioLocal)) {
                return;
            }

            int indiceAtual =
                    gerenciadorNavegacaoGif.getIndiceAtual();
            int proximoIndice = indiceAtual < 0
                    ? 0
                    : (indiceAtual + 1) % nomes.size();
            solicitarPreparacaoGifSincronizado(proximoIndice);
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

        if (movie != null) {
            exibirSegmentoLinhaDoTempo(indiceSelecionado);
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

        indiceInicialLinhaDoTempo = posicao;
        if (movie != null) {
            exibirSegmentoLinhaDoTempo(posicao);
            return;
        }
        if (preparandoLinhaDoTempo) {
            status.setText("Preparando a linha do tempo dos GIFs...");
            return;
        }

        carregando = true;
        preparandoLinhaDoTempo = true;
        status.setText("Preparando a linha do tempo dos GIFs...");
        long geracao = ++geracaoLinhaDoTempo;
        gerenciadorDecodificacaoGif.prepararLinhaDoTempo(
                links,
                repeticoesCiclos,
                gifRepository,
                new GerenciadorDecodificacaoGif.TimelineListener() {
                    @Override
                    public void onTimelineReady(GifFrameSequence timeline) {
                        if (!atividadeDisponivel() ||
                                geracao != geracaoLinhaDoTempo) {
                            timeline.liberar();
                            return;
                        }

                        preparandoLinhaDoTempo = false;
                        carregando = false;
                        GifFrameSequence sequenciaAnterior = movie;
                        movie = timeline;
                        if (sequenciaAnterior != null) {
                            sequenciaAnterior.liberar();
                        }
                        if (aguardandoInicioSincronizado) {
                            prepararExibicaoSincronizada(
                                    indiceGifSincronizado
                            );
                        } else {
                            exibirSegmentoLinhaDoTempo(
                                    indiceInicialLinhaDoTempo
                            );
                        }
                    }

                    @Override
                    public void onErro(Exception erro) {
                        if (!atividadeDisponivel() ||
                                geracao != geracaoLinhaDoTempo) {
                            return;
                        }

                        preparandoLinhaDoTempo = false;
                        carregando = false;
                        status.setText(
                                "Erro ao preparar os GIFs: " +
                                        erro.getMessage()
                        );
                        if (aguardandoInicioSincronizado) {
                            enviarFalhaPreparacaoSincronizada(
                                    revisaoSincronizacao,
                                    "Não foi possível preparar a linha do tempo."
                            );
                        }
                        Log.e(TAG, "Falha ao unir os quadros dos GIFs.", erro);
                    }
                }
        );
    }

    private void prepararExibicaoSincronizada(int indice) {
        if (movie == null ||
                !gerenciadorNavegacaoGif.selecionar(indice, nomes.size())) {
            return;
        }

        gerenciadorReproducao.reiniciar();
        gerenciadorReproducao.definirSequencia(movie);
        progressoControlesAtual = movie.getProgressForSegment(indice);
        gerenciadorReproducao.definirIndiceQuadro(
                movie.getFrameForSegment(indice)
        );
        indiceSegmentoExibido = indice;
        desenharQuadro();
        String texto = obterTextoSegmento(indice);
        gerenciadorTextoPlanilha.iniciarComVisibilidadeExterna(texto);
        status.setText(
                nomes.get(indice) + " | " +
                        (indice + 1) + "/" + nomes.size()
        );
        enviarProntoParaSincronizacao();
    }

    private void exibirSegmentoLinhaDoTempo(int indice) {
        if (movie == null ||
                !gerenciadorNavegacaoGif.selecionar(indice, nomes.size())) {
            return;
        }

        gerenciadorReproducao.pausar();
        gerenciadorReproducao.definirSequencia(movie);
        progressoControlesAtual = movie.getProgressForSegment(indice);
        gerenciadorReproducao.definirIndiceQuadro(
                movie.getFrameForSegment(indice)
        );
        indiceSegmentoExibido = -1;
        gerenciadorMovimento.reiniciarContagem();
        gerenciadorControle.moverControles(progressoControlesAtual);
        desenharQuadro();
        atualizarSegmentoLinhaDoTempo();
        iniciarAnimacao();
    }

    private String obterTextoSegmento(int indice) {
        return indice >= 0 && indice < textosPlanilha.size()
                ? textosPlanilha.get(indice)
                : "";
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    @Override
    protected void onDestroy() {

        atividadeEncerrada = true;

        geracaoLinhaDoTempo++;
        preparandoLinhaDoTempo = false;
        GifFrameSequence sequenciaAtual = movie;

        if (gifTiming != null) {

            gifTiming.limpar();
        }

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

        if (!isChangingConfigurations()) {
            if (ConexaoActivity.conexao != null) {
                ConexaoActivity.conexao.desconectar();
                ConexaoActivity.conexao = null;
            }
        }

        // -------------------------------------------------
        // LIBERAR RENDERIZADOR
        // -------------------------------------------------

        if (renderizadorGif != null) {

            renderizadorGif.liberar();

            renderizadorGif = null;
        }

        if (sequenciaAtual != null) {
            sequenciaAtual.liberar();
        }

        // -------------------------------------------------
        // ENCERRAR EXECUTOR
        // -------------------------------------------------

        executor.shutdownNow();

        super.onDestroy();
    }
}