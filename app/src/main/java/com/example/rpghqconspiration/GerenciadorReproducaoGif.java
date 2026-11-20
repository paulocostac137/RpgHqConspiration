package com.example.rpghqconspiration;

import android.os.Handler;
import android.os.SystemClock;

/** Advances through every frame in the combined GIF timeline. */
public final class GerenciadorReproducaoGif {

    public interface Listener {
        boolean podeIniciar();
        void atualizarQuadro();
        default void onControleAlinhado() {
        }
    }

    private final AnimationTicker animationTicker;
    private final Listener listener;

    private GifFrameSequence sequencia;
    private float progresso;
    private int indiceQuadro;
    private int quadrosAvancados;
    private long tempoAcumuladoMs;
    private long inicioReproducao;
    private boolean reproduzindo;
    private boolean alinhandoControle;
    private int segmentoAlinhamento = -1;
    private final GifCycleAlignment alinhamentoCiclo =
            new GifCycleAlignment();

    public GerenciadorReproducaoGif(
            Handler handler,
            long intervalo,
            Listener listener) {
        if (handler == null || listener == null) {
            throw new IllegalArgumentException(
                    "Handler e Listener são obrigatórios."
            );
        }

        this.listener = listener;
        animationTicker = new AnimationTicker(
                handler,
                this::atualizar,
                intervalo
        );
    }

    private void atualizar() {
        if (!reproduzindo) {
            return;
        }

        long agora = SystemClock.uptimeMillis();
        long tempoDecorridoMs = Math.max(0L, agora - inicioReproducao);
        inicioReproducao = agora;

        float fatorVelocidadeRecuperacao =
                alinhamentoCiclo.isRecovering()
                        ? alinhamentoCiclo.updateRecovery(agora)
                        : 1f;
        float fatorVelocidade = alinhandoControle
                ? alinhamentoCiclo.updateSpeed(
                        obterOffsetQuadroCiclo(),
                        agora
                )
                : fatorVelocidadeRecuperacao;
        tempoAcumuladoMs += Math.round(tempoDecorridoMs * fatorVelocidade);

        int totalQuadros = sequencia.getFrameCount();
        boolean controleAlinhado = false;
        while (true) {
            if (alinhandoControle && estaNoQuadroAlvo()) {
                tempoAcumuladoMs = 0L;
                alinhandoControle = false;
                controleAlinhado = true;
                break;
            }
            long duracaoQuadro = obterDuracaoQuadro(quadrosAvancados);
            if (tempoAcumuladoMs < duracaoQuadro) {
                break;
            }

            tempoAcumuladoMs -= duracaoQuadro;
            if (alinhandoControle) {
                int inicioSegmento =
                        sequencia.getCycleStartFrame(segmentoAlinhamento);
                int fimSegmento = inicioSegmento +
                        sequencia.getCycleTotalFrameCount(
                                segmentoAlinhamento
                        );
                quadrosAvancados = quadrosAvancados + 1 >= fimSegmento
                        ? inicioSegmento
                        : quadrosAvancados + 1;
            } else {
                quadrosAvancados = (quadrosAvancados + 1) % totalQuadros;
            }

            if (alinhandoControle && estaNoQuadroAlvo()) {
                tempoAcumuladoMs = 0L;
                alinhandoControle = false;
                controleAlinhado = true;
                break;
            }
        }

        if (!reproduzindo) {
            return;
        }
        indiceQuadro = quadrosAvancados;
        progresso = (float) indiceQuadro / totalQuadros;
        listener.atualizarQuadro();
        if (controleAlinhado) {
            listener.onControleAlinhado();
        }
    }

    private long obterDuracaoQuadro(int indice) {
        int totalQuadros = sequencia.getFrameCount();
        long duracaoTotal = sequencia.getDurationMs();
        long inicio = (long) indice * duracaoTotal / totalQuadros;
        long fim = (long) (indice + 1) * duracaoTotal / totalQuadros;
        return Math.max(1L, fim - inicio);
    }

    public void iniciar(GifFrameSequence sequencia) {
        if (sequencia == null ||
                reproduzindo ||
                !listener.podeIniciar()) {
            return;
        }

        if (this.sequencia != sequencia) {
            definirSequencia(sequencia);
        }
        reproduzindo = true;
        inicioReproducao = SystemClock.uptimeMillis();
        animationTicker.iniciar();
    }

    public void iniciarDuranteControle() {
        if (sequencia == null || reproduzindo) {
            return;
        }
        reproduzindo = true;
        inicioReproducao = SystemClock.uptimeMillis();
        animationTicker.iniciar();
    }

    public void pausar() {
        reproduzindo = false;
        animationTicker.parar();
    }

    public void definirSequencia(GifFrameSequence sequencia) {
        cancelarAlinhamentoControle();
        this.sequencia = sequencia;
        indiceQuadro = sequencia == null
                ? 0
                : sequencia.getFrameIndex(progresso);
        quadrosAvancados = indiceQuadro;
        tempoAcumuladoMs = 0L;
    }

    public int getIndiceQuadro() {
        return indiceQuadro;
    }

    public float getProgresso() {
        return progresso;
    }

    public void definirProgresso(float progresso) {
        this.progresso = Math.max(0f, Math.min(1f, progresso));
        if (sequencia != null) {
            indiceQuadro = sequencia.getFrameIndex(this.progresso);
            quadrosAvancados = indiceQuadro;
            tempoAcumuladoMs = 0L;
        }
    }

    public void definirIndiceQuadro(int indice) {
        if (sequencia == null) {
            return;
        }

        indiceQuadro = Math.max(
                0,
                Math.min(sequencia.getFrameCount() - 1, indice)
        );
        quadrosAvancados = indiceQuadro;
        progresso = (float) indiceQuadro / sequencia.getFrameCount();
        tempoAcumuladoMs = 0L;
    }

    public boolean alinharComQuadroDoCiclo(
            int segmentIndex,
            int targetCycleFrameOffset) {
        if (sequencia == null) {
            throw new IllegalStateException(
                    "Não há sequência de GIF carregada."
            );
        }
        int cycleFrameCount = sequencia.getCycleFrameCount(segmentIndex);
        if (targetCycleFrameOffset < 0 ||
                targetCycleFrameOffset >= cycleFrameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro alvo não pertence ao ciclo selecionado."
            );
        }

        int currentCycleOffset = sequencia.getCycleOffsetForFrame(
                segmentIndex,
                quadrosAvancados
        );
        if (segmentIndex != segmentoAlinhamento || !alinhandoControle) {
            if (currentCycleOffset == targetCycleFrameOffset) {
                cancelarAlinhamentoControle();
                return false;
            }
            segmentoAlinhamento = segmentIndex;
            alinhamentoCiclo.begin(
                    cycleFrameCount,
                    sequencia.getCycleDurationMs(segmentIndex),
                    currentCycleOffset,
                    targetCycleFrameOffset,
                    SystemClock.uptimeMillis()
            );
        } else {
            alinhamentoCiclo.updateTarget(
                    targetCycleFrameOffset,
                    SystemClock.uptimeMillis()
            );
        }
        if (estaNoQuadroAlvo()) {
            alinhandoControle = false;
            tempoAcumuladoMs = 0L;
            return false;
        }
        alinhandoControle = true;
        return true;
    }

    public boolean isAlinhandoControle() {
        return alinhandoControle;
    }

    public void retomarAposControle() {
        alinhandoControle = false;
        segmentoAlinhamento = -1;
        alinhamentoCiclo.beginRecovery(SystemClock.uptimeMillis());
        tempoAcumuladoMs = 0L;
        if (reproduzindo) {
            inicioReproducao = SystemClock.uptimeMillis();
        } else if (sequencia != null) {
            reproduzindo = true;
            inicioReproducao = SystemClock.uptimeMillis();
            animationTicker.iniciar();
        }
    }

    public void cancelarAlinhamentoControle() {
        alinhandoControle = false;
        segmentoAlinhamento = -1;
        alinhamentoCiclo.reset();
        tempoAcumuladoMs = 0L;
        if (reproduzindo) {
            inicioReproducao = SystemClock.uptimeMillis();
        }
    }

    private boolean estaNoQuadroAlvo() {
        return sequencia.getCycleIndexForFrame(quadrosAvancados)
                        == segmentoAlinhamento &&
                sequencia.getCycleOffsetForFrame(
                        segmentoAlinhamento,
                        quadrosAvancados
                ) == alinhamentoCiclo.getTargetFrame();
    }

    private int obterOffsetQuadroCiclo() {
        return sequencia.getCycleOffsetForFrame(
                segmentoAlinhamento,
                quadrosAvancados
        );
    }

    public boolean isReproduzindo() {
        return reproduzindo;
    }

    public void reiniciar() {
        animationTicker.parar();
        reproduzindo = false;
        progresso = 0f;
        indiceQuadro = 0;
        quadrosAvancados = 0;
        tempoAcumuladoMs = 0L;
        inicioReproducao = 0L;
        cancelarAlinhamentoControle();
        sequencia = null;
    }

    public void encerrar() {
        animationTicker.parar();
        reproduzindo = false;
        inicioReproducao = 0L;
        cancelarAlinhamentoControle();
    }
}
