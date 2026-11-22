package com.example.rpghqconspiration;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public final class PreviewGifActivity extends AppCompatActivity {

    private GifFrameSequence sequencia;
    private ImageView imagemGif;
    private FrameLayout areaGif;
    private LinearLayout linhaControles;
    private TextView controleEsquerdo;
    private TextView controleDireito;
    private FrameLayout containerControleEsquerdo;
    private FrameLayout containerControleDireito;
    private View sombraSuperiorEsquerda;
    private View sombraInferiorEsquerda;
    private View sombraSuperiorDireita;
    private View sombraInferiorDireita;
    private GerenciadorControleMultitoque gerenciadorControle;
    private GerenciadorReproducaoGif gerenciadorReproducao;
    private int quadroBaseControle;
    private float progressoBaseControle;
    private float progressoControlesAtual = 0.5f;
    private int cicloControleAtual = -1;
    private boolean aguardandoProgressoInicial;
    private boolean faixaOnOutAtiva;
    private boolean faixaControleAguardandoDirecaoNoOn;
    private boolean sombraInferiorAtravessada;
    private boolean sombraSuperiorAtravessada;
    private boolean cicloControleAtravessadoPeloTopo;
    private boolean destruindo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_gif);
        ModoImersivo.ativar(getWindow());

        Button botaoVoltar = findViewById(R.id.botaoVoltarPreview);
        Button botaoPov = findViewById(R.id.botaoPovPreview);
        areaGif = findViewById(R.id.areaGifPreview);
        linhaControles = findViewById(R.id.linhaControlesPreview);
        controleEsquerdo = findViewById(R.id.controleEsquerdoPreview);
        controleDireito = findViewById(R.id.controleDireitoPreview);
        imagemGif = findViewById(R.id.imagemGifPreview);
        containerControleEsquerdo =
                findViewById(R.id.containerControleEsquerdoPreview);
        containerControleDireito =
                findViewById(R.id.containerControleDireitoPreview);
        sombraSuperiorEsquerda =
                findViewById(R.id.sombraSuperiorEsquerdaPreview);
        sombraInferiorEsquerda =
                findViewById(R.id.sombraInferiorEsquerdaPreview);
        sombraSuperiorDireita =
                findViewById(R.id.sombraSuperiorDireitaPreview);
        sombraInferiorDireita =
                findViewById(R.id.sombraInferiorDireitaPreview);
        configurarEstiloControles();
        configurarAfastamentoControles();
        botaoVoltar.setOnClickListener(view -> finish());
        botaoPov.setOnClickListener(view -> alternarPontoDeVista(botaoPov));

        sequencia = ResultadoProcessamentoGif.consumirPreview();
        if (sequencia == null || sequencia.getFrameCount() == 0) {
            Toast.makeText(
                    this,
                    "Não foi possível carregar o preview.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        gerenciadorReproducao = new GerenciadorReproducaoGif(
                new Handler(Looper.getMainLooper()),
                16L,
                new GerenciadorReproducaoGif.Listener() {
                    @Override
                    public boolean podeIniciar() {
                        return sequencia != null && !destruindo;
                    }

                    @Override
                    public void atualizarQuadro() {
                        mostrarQuadro(
                                gerenciadorReproducao.getIndiceQuadro()
                        );
                    }
                }
        );
        gerenciadorReproducao.definirSequencia(sequencia);
        gerenciadorReproducao.definirIndiceQuadro(0);

        gerenciadorControle = new GerenciadorControleMultitoque(
                areaGif,
                controleEsquerdo,
                controleDireito,
                new GerenciadorControleMultitoque.Listener() {
                    @Override
                    public boolean possuiGif() {
                        return sequencia != null;
                    }

                    @Override
                    public void onControleManualIniciado() {
                        gerenciadorReproducao.pausar();
                        quadroBaseControle =
                                gerenciadorReproducao.getIndiceQuadro();
                        aguardandoProgressoInicial = true;
                        inicializarFaixaControle(
                                progressoControlesAtual,
                                true
                        );
                    }

                    @Override
                    public void onProgressoManual(float progresso) {
                        if (aguardandoProgressoInicial) {
                            progressoBaseControle = progresso;
                            progressoControlesAtual = progresso;
                            inicializarFaixaControle(progresso, true);
                            sombraInferiorAtravessada =
                                    GifFramePositionMapper
                                            .entrouNaSombraInferior(progresso);
                            sombraSuperiorAtravessada =
                                    GifFramePositionMapper
                                            .entrouNaSombraSuperior(progresso);
                            aguardandoProgressoInicial = false;
                            return;
                        }
                        if (Math.abs(progresso - progressoBaseControle) <
                                0.002f) {
                            gerenciadorReproducao.definirIndiceQuadro(
                                    quadroBaseControle
                            );
                            mostrarQuadro(quadroBaseControle);
                            return;
                        }
                        if (faixaControleAguardandoDirecaoNoOn) {
                            faixaOnOutAtiva =
                                    progresso < progressoBaseControle &&
                                            sequencia.hasRangeAfterOn(
                                                    cicloControleAtual
                                            );
                            faixaControleAguardandoDirecaoNoOn = false;
                            atualizarCoresSombras();
                        }
                        float progressoQuadro =
                                atualizarFaixaControlePorProgresso(progresso);
                        int deslocamento = sequencia
                                .getCycleFrameOffsetForControlProgress(
                                        cicloControleAtual,
                                        progressoQuadro,
                                        faixaOnOutAtiva
                                );
                        int quadroAlvo = sequencia.getCycleStartFrame(
                                cicloControleAtual
                        ) + deslocamento;
                        gerenciadorReproducao.definirIndiceQuadro(quadroAlvo);
                        progressoControlesAtual = progresso;
                        mostrarQuadro(
                                gerenciadorReproducao.getIndiceQuadro()
                        );
                    }

                    @Override
                    public void onControleManualLiberado() {
                        resetarEstadoControle();
                        if (!destruindo) {
                            gerenciadorReproducao.iniciar(sequencia);
                        }
                    }

                    @Override
                    public void onSolicitacaoTrocaIniciada() {
                    }

                    @Override
                    public void onSolicitacaoTrocaCancelada() {
                    }

                    @Override
                    public void onSolicitacaoTrocaConcluida() {
                    }
                }
        );
        gerenciadorControle.configurar();
        gerenciadorControle.definirHabilitado(false);
        mostrarQuadro(0);
        gerenciadorReproducao.iniciar(sequencia);
    }

    private void alternarPontoDeVista(Button botaoPov) {
        boolean povBT = "PovAF".contentEquals(botaoPov.getText());
        botaoPov.setText(povBT ? "PovBT" : "PovAF");
        imagemGif.setRotation(povBT ? 180f : 0f);
        imagemGif.setScaleX(povBT ? -1f : 1f);
    }

    private void configurarEstiloControles() {
        int largura = dp(48);
        int altura = dp(100);
        float raio = dp(8);
        for (TextView controle : new TextView[]{
                controleEsquerdo,
                controleDireito
        }) {
            FrameLayout.LayoutParams parametros =
                    (FrameLayout.LayoutParams) controle.getLayoutParams();
            parametros.width = largura;
            parametros.height = altura;
            boolean esquerda =
                    controle.getId() == R.id.controleEsquerdoPreview;
            parametros.gravity = Gravity.TOP |
                    (esquerda ? Gravity.START : Gravity.END);
            parametros.topMargin = 0;
            parametros.bottomMargin = 0;
            parametros.leftMargin = 0;
            parametros.rightMargin = 0;
            controle.setLayoutParams(parametros);

            GradientDrawable fundo = new GradientDrawable();
            fundo.setColor(0xCC222222);
            fundo.setCornerRadius(raio);
            controle.setBackground(fundo);
        }

        definirEstiloSombra(sombraSuperiorEsquerda, raio, true, true);
        definirEstiloSombra(sombraInferiorEsquerda, raio, false, false);
        definirEstiloSombra(sombraSuperiorDireita, raio, true, true);
        definirEstiloSombra(sombraInferiorDireita, raio, false, false);
    }

    private void definirEstiloSombra(
            View sombra,
            float raio,
            boolean superior,
            boolean azul) {
        int cor = azul ? 0xFF2196F3 : 0xFFF44336;
        int[] cores = {
                (cor & 0x00FFFFFF) | 0xCC000000,
                (cor & 0x00FFFFFF) | 0x66000000,
                cor & 0x00FFFFFF
        };
        GradientDrawable degrade = new GradientDrawable(
                superior
                        ? GradientDrawable.Orientation.BOTTOM_TOP
                        : GradientDrawable.Orientation.TOP_BOTTOM,
                cores
        );
        degrade.setCornerRadii(superior
                ? new float[]{
                        0f, 0f, 0f, 0f,
                        raio, raio, raio, raio
                }
                : new float[]{
                        raio, raio, raio, raio,
                        0f, 0f, 0f, 0f
                });
        sombra.setBackground(degrade);
    }

    private void configurarAfastamentoControles() {
        ((ViewGroup) linhaControles).setMotionEventSplittingEnabled(false);
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
                    atualizarPosicaoSombrasControles();
                }
        );
        View.OnLayoutChangeListener listenerSombras =
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) ->
                        atualizarPosicaoSombrasControles();
        areaGif.addOnLayoutChangeListener(listenerSombras);
        controleEsquerdo.addOnLayoutChangeListener(listenerSombras);
        controleDireito.addOnLayoutChangeListener(listenerSombras);
    }

    private void atualizarPosicaoSombrasControles() {
        atualizarPosicaoSombra(
                controleEsquerdo,
                sombraSuperiorEsquerda,
                true,
                false
        );
        atualizarPosicaoSombra(
                controleEsquerdo,
                sombraInferiorEsquerda,
                true,
                true
        );
        atualizarPosicaoSombra(
                controleDireito,
                sombraSuperiorDireita,
                false,
                false
        );
        atualizarPosicaoSombra(
                controleDireito,
                sombraInferiorDireita,
                false,
                true
        );
    }

    private void atualizarPosicaoSombra(
            TextView controle,
            View sombra,
            boolean esquerda,
            boolean inferior) {
        if (controle.getWidth() <= 0 ||
                controle.getHeight() <= 0 ||
                areaGif.getHeight() <= controle.getHeight()) {
            return;
        }

        int alturaSombra = Math.max(
                1,
                Math.round(
                        (areaGif.getHeight() - controle.getHeight()) * 0.10f
                )
        );
        FrameLayout.LayoutParams parametros =
                new FrameLayout.LayoutParams(
                        controle.getWidth(),
                        alturaSombra
                );
        parametros.gravity =
                (inferior ? Gravity.BOTTOM : Gravity.TOP) |
                        (esquerda ? Gravity.START : Gravity.END);
        sombra.setLayoutParams(parametros);
        sombra.setTranslationX(controle.getTranslationX());
    }

    private int dp(float valor) {
        return Math.round(
                valor * getResources().getDisplayMetrics().density
        );
    }

    private void mostrarQuadro(int indice) {
        if (sequencia == null) {
            return;
        }
        int quadro = Math.max(
                0,
                Math.min(sequencia.getFrameCount() - 1, indice)
        );
        Bitmap bitmap = sequencia.getFrame(quadro);
        imagemGif.setImageBitmap(bitmap);

        boolean controlesVisiveis =
                sequencia.isFrameProvenienteDeMarcadores(quadro);
        int visibilidade = controlesVisiveis ? View.VISIBLE : View.GONE;
        containerControleEsquerdo.setVisibility(visibilidade);
        containerControleDireito.setVisibility(visibilidade);
        atualizarVisibilidadeSombras(quadro, controlesVisiveis);
        gerenciadorControle.definirHabilitado(
                controlesVisiveis ||
                        gerenciadorControle.isControleManualAtivo()
        );
    }

    private void inicializarFaixaControle(
            float progresso,
            boolean aguardarDirecaoNoOn) {
        cicloControleAtravessadoPeloTopo = false;
        int quadro = gerenciadorReproducao.getIndiceQuadro();
        cicloControleAtual = sequencia.getControlCycleIndexForFrame(quadro);
        int deslocamentoOn =
                sequencia.getCycleOnFrameOffset(cicloControleAtual);
        int deslocamentoAtual =
                quadro - sequencia.getCycleStartFrame(cicloControleAtual);
        faixaControleAguardandoDirecaoNoOn =
                aguardarDirecaoNoOn &&
                        deslocamentoOn >= 0 &&
                        deslocamentoAtual == deslocamentoOn;
        faixaOnOutAtiva = deslocamentoOn >= 0 &&
                GifFramePositionMapper.controlUsesOnOutRange(
                        deslocamentoAtual,
                        deslocamentoOn,
                        false
                );
        if (!sequencia.hasRangeAfterOn(cicloControleAtual)) {
            faixaOnOutAtiva = false;
            faixaControleAguardandoDirecaoNoOn = false;
        }
        sombraInferiorAtravessada =
                GifFramePositionMapper.entrouNaSombraInferior(progresso);
        sombraSuperiorAtravessada =
                GifFramePositionMapper.entrouNaSombraSuperior(progresso);
        atualizarCoresSombras();
        atualizarVisibilidadeSombras(quadro, true);
    }

    private float atualizarFaixaControlePorProgresso(float progresso) {
        if (GifFramePositionMapper.entrouNaSombraInferior(progresso)) {
            if (!sombraInferiorAtravessada) {
                sombraInferiorAtravessada = true;
                if (faixaOnOutAtiva) {
                    if (sequencia.hasRangeBeforeOn(cicloControleAtual)) {
                        faixaOnOutAtiva = false;
                    }
                } else if (sequencia.hasRangeAfterOn(cicloControleAtual)) {
                    faixaOnOutAtiva = true;
                }
                atualizarCoresSombras();
                atualizarVisibilidadeSombras(
                        gerenciadorReproducao.getIndiceQuadro(),
                        true
                );
            }
        } else {
            sombraInferiorAtravessada = false;
        }

        if (GifFramePositionMapper.entrouNaSombraSuperior(progresso)) {
            if (!sombraSuperiorAtravessada) {
                sombraSuperiorAtravessada = true;
                int cicloAdjacente = faixaOnOutAtiva
                        ? cicloControleAtual + 1
                        : cicloControleAtual - 1;
                if (cicloAdjacente >= 0 &&
                        cicloAdjacente < sequencia.getCycleCount()) {
                    int quadroLimite = faixaOnOutAtiva
                            ? sequencia.getCycleStartFrame(cicloAdjacente)
                            : sequencia.getCycleStartFrame(cicloAdjacente) +
                                    sequencia.getCycleFrameCount(
                                            cicloAdjacente
                                    ) - 1;
                    if (sequencia.isFrameProvenienteDeMarcadores(
                            quadroLimite
                    )) {
                        faixaOnOutAtiva = !faixaOnOutAtiva;
                        cicloControleAtual = cicloAdjacente;
                        cicloControleAtravessadoPeloTopo = true;
                        gerenciadorReproducao.definirIndiceQuadro(
                                quadroLimite
                        );
                        atualizarCoresSombras();
                    }
                }
                atualizarVisibilidadeSombras(
                        gerenciadorReproducao.getIndiceQuadro(),
                        true
                );
            }
        } else {
            sombraSuperiorAtravessada = false;
        }

        return cicloControleAtravessadoPeloTopo
                ? GifFramePositionMapper.progressoAposSombraSuperior(
                        progresso
                )
                : progresso;
    }

    private void atualizarVisibilidadeSombras(
            int quadro,
            boolean controlesLiberados) {
        if (!controlesLiberados ||
                !sequencia.isFrameProvenienteDeMarcadores(quadro)) {
            definirVisibilidadeSombras(View.GONE);
            return;
        }
        int ciclo = sequencia.getControlCycleIndexForFrame(quadro);
        boolean podeAlternarInferior = faixaOnOutAtiva
                ? sequencia.hasRangeBeforeOn(ciclo)
                : sequencia.hasRangeAfterOn(ciclo);
        boolean podeAlternarSuperior = faixaOnOutAtiva
                ? sequencia.hasRangeAfterOn(ciclo)
                : sequencia.hasRangeBeforeOn(ciclo);
        int cicloAdjacente = faixaOnOutAtiva ? ciclo + 1 : ciclo - 1;
        if (cicloAdjacente < 0 ||
                cicloAdjacente >= sequencia.getCycleCount()) {
            podeAlternarSuperior = false;
        } else {
            int quadroLimite = faixaOnOutAtiva
                    ? sequencia.getCycleStartFrame(cicloAdjacente)
                    : sequencia.getCycleStartFrame(cicloAdjacente) +
                            sequencia.getCycleFrameCount(cicloAdjacente) - 1;
            podeAlternarSuperior = podeAlternarSuperior &&
                    sequencia.isFrameProvenienteDeMarcadores(quadroLimite);
        }
        sombraInferiorEsquerda.setVisibility(
                podeAlternarInferior ? View.VISIBLE : View.GONE
        );
        sombraInferiorDireita.setVisibility(
                podeAlternarInferior ? View.VISIBLE : View.GONE
        );
        sombraSuperiorEsquerda.setVisibility(
                podeAlternarSuperior ? View.VISIBLE : View.GONE
        );
        sombraSuperiorDireita.setVisibility(
                podeAlternarSuperior ? View.VISIBLE : View.GONE
        );
    }

    private void definirVisibilidadeSombras(int visibilidade) {
        sombraInferiorEsquerda.setVisibility(visibilidade);
        sombraInferiorDireita.setVisibility(visibilidade);
        sombraSuperiorEsquerda.setVisibility(visibilidade);
        sombraSuperiorDireita.setVisibility(visibilidade);
    }

    private void atualizarCoresSombras() {
        definirEstiloSombra(
                sombraInferiorEsquerda,
                dp(8),
                false,
                faixaOnOutAtiva
        );
        definirEstiloSombra(
                sombraInferiorDireita,
                dp(8),
                false,
                faixaOnOutAtiva
        );
        definirEstiloSombra(
                sombraSuperiorEsquerda,
                dp(8),
                true,
                !faixaOnOutAtiva
        );
        definirEstiloSombra(
                sombraSuperiorDireita,
                dp(8),
                true,
                !faixaOnOutAtiva
        );
    }

    private void resetarEstadoControle() {
        faixaOnOutAtiva = false;
        faixaControleAguardandoDirecaoNoOn = false;
        sombraInferiorAtravessada = false;
        sombraSuperiorAtravessada = false;
        cicloControleAtravessadoPeloTopo = false;
        cicloControleAtual = -1;
        atualizarCoresSombras();
        int quadro = gerenciadorReproducao.getIndiceQuadro();
        atualizarVisibilidadeSombras(
                quadro,
                sequencia.isFrameProvenienteDeMarcadores(quadro)
        );
    }

    @Override
    protected void onPause() {
        if (gerenciadorReproducao != null) {
            gerenciadorReproducao.pausar();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gerenciadorReproducao != null &&
                gerenciadorControle != null &&
                !gerenciadorControle.isControleManualAtivo()) {
            gerenciadorReproducao.iniciar(sequencia);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !destruindo) {
            ModoImersivo.ativar(getWindow());
        }
    }

    @Override
    protected void onDestroy() {
        destruindo = true;
        if (gerenciadorReproducao != null) {
            gerenciadorReproducao.encerrar();
        }
        if (gerenciadorControle != null) {
            gerenciadorControle.definirHabilitado(false);
        }
        if (imagemGif != null) {
            imagemGif.setImageDrawable(null);
        }
        if (sequencia != null) {
            sequencia.liberar();
            sequencia = null;
        }
        ResultadoProcessamentoGif.limparPreview();
        super.onDestroy();
    }
}
