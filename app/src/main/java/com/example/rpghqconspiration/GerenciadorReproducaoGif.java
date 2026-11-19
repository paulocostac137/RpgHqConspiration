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
        tempoAcumuladoMs += agora - inicioReproducao;
        inicioReproducao = agora;

        float fatorVelocidadeRecuperacao =
                alinhamentoCiclo.isRecovering()
                        ? alinhamentoCiclo.updateRecovery(agora)
                        : 1f;
        int totalQuadros = sequencia.getFrameCount();
        boolean controleAlinhado = false;
        while (true) {
            long duracaoQuadro = obterDuracaoQuadro(quadrosAvancados);
            if (alinhandoControle) {
                duracaoQuadro = Math.max(
                        1L,
                        Math.round(
                                duracaoQuadro /
                                        alinhamentoCiclo.updateSpeed(
                                                obterOffsetQuadroCiclo(),
                                                agora
                                        )
                        )
                );
            } else if (alinhamentoCiclo.isRecovering()) {
                duracaoQuadro = Math.max(
                        1L,
                        Math.round(
                                duracaoQuadro /
                                        fatorVelocidadeRecuperacao
                        )
                );
            }
            if (tempoAcumuladoMs < duracaoQuadro) {
                break;
            }

            tempoAcumuladoMs -= duracaoQuadro;
            if (alinhandoControle) {
                int inicioSegmento =
                        sequencia.getSegmentStartFrame(segmentoAlinhamento);
                int fimSegmento = inicioSegmento +
                        sequencia.getSegmentTotalFrameCount(
                                segmentoAlinhamento
                        );
                quadrosAvancados = quadrosAvancados + 1 >= fimSegmento
                        ? inicioSegmento
                        : quadrosAvancados + 1;
            } else {
                quadrosAvancados = (quadrosAvancados + 1) % totalQuadros;
            }

            if (alinhandoControle && estaNoQuadroAlvo()) {
                alinhandoControle = false;
                segmentoAlinhamento = -1;
                tempoAcumuladoMs = 0L;
                controleAlinhado = true;
                break;
            }
        }

        if (!reproduzindo) {
            return;
        }
        indiceQuadro = quadrosAvancados;
        progresso = (float) indiceQuadro / totalQuadros;
        if (controleAlinhado) {
            listener.onControleAlinhado();
        }
        listener.atualizarQuadro();
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
        int cycleFrameCount = sequencia.getSegmentFrameCount(segmentIndex);
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
                    sequencia.getSegmentCycleDurationMs(segmentIndex),
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
            cancelarAlinhamentoControle();
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
        return sequencia.getSegmentIndex(quadrosAvancados)
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
