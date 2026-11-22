package com.example.rpghqconspiration;

import android.util.Log;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

/**
 * Responsável exclusivamente pelo gerenciamento
 * dos controles multitouch.
 *
 * Não controla GIFs, servidor ou reprodução.
 */
public final class GerenciadorControleMultitoque {

    private static final String TAG =
            "GerenciadorMultitoque";
    private static final long DURACAO_TROCA_CONTROLE_MS = 2000L;
    private static final long DURACAO_RETORNO_CONTROLES_MS = 450L;

    // =====================================================
    // COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        boolean possuiGif();

        void onControleManualIniciado();

        void onProgressoManual(float progresso);

        void onControleManualLiberado();

        void onSolicitacaoTrocaIniciada();

        void onSolicitacaoTrocaCancelada();

        void onSolicitacaoTrocaConcluida();
    }

    private final FrameLayout areaGif;

    private final TextView controleEsquerdo;

    private final TextView controleDireito;

    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // =====================================================
    // ESTADO DOS DEDOS
    // =====================================================

    private boolean esquerdoPressionado = false;

    private boolean direitoPressionado = false;

    private boolean controleManualAtivo = false;

    private boolean habilitado = true;
    private boolean modoSolicitacao = false;
    private boolean solicitacaoEmAndamento = false;
    private boolean solicitacaoConcluida = false;
    private boolean aguardandoFimSolicitacao = false;

    private float posicaoDedoEsquerdo = 0;
    private float posicaoDedoDireito = 0;

    private int idDedoEsquerdo = -1;

    private int idDedoDireito = -1;

    private float progressoVisualControles = 0.5f;
    private ValueAnimator animacaoRetornoControles;

    private final Runnable concluirSolicitacao =
            this::concluirSolicitacao;

    private void concluirSolicitacao() {
        if (!habilitado || !modoSolicitacao ||
                !esquerdoPressionado || !direitoPressionado) {
            solicitacaoEmAndamento = false;
            return;
        }

        solicitacaoEmAndamento = false;
        solicitacaoConcluida = true;
        aguardandoFimSolicitacao = true;
        liberarTodosOsDedos();
        listener.onSolicitacaoTrocaConcluida();
    }

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorControleMultitoque(
            FrameLayout areaGif,
            TextView controleEsquerdo,
            TextView controleDireito,
            Listener listener) {

        if (areaGif == null ||
                controleEsquerdo == null ||
                controleDireito == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "Os componentes e o Listener são obrigatórios."
            );
        }

        this.areaGif = areaGif;
        this.controleEsquerdo = controleEsquerdo;
        this.controleDireito = controleDireito;
        this.listener = listener;
    }

    // =====================================================
    // CONFIGURAÇÃO DOS CONTROLES
    // =====================================================

    public void configurar() {

        if (!(areaGif.getParent() instanceof ViewGroup)) {
            throw new IllegalStateException(
                    "A área do GIF deve estar dentro de um contêiner de controles."
            );
        }

        ViewGroup linhaControles =
                (ViewGroup) areaGif.getParent();
        linhaControles.setMotionEventSplittingEnabled(false);

        View.OnTouchListener listenerMultitoque =
                (view, evento) -> {

                    processarEventoMultitoque(
                            view,
                            evento
                    );

                    return true;
                };

        controleEsquerdo.setOnTouchListener(null);
        controleDireito.setOnTouchListener(null);
        linhaControles.setOnTouchListener(listenerMultitoque);
        centralizarControles();
    }

    // =====================================================
    // PROCESSAMENTO MULTITOQUE
    // =====================================================

    private void processarEventoMultitoque(
            View view,
            MotionEvent evento) {

        int acao = evento.getActionMasked();

        if (aguardandoFimSolicitacao) {
            if (acao == MotionEvent.ACTION_UP ||
                    acao == MotionEvent.ACTION_CANCEL) {
                aguardandoFimSolicitacao = false;
                solicitacaoConcluida = false;
            }
            return;
        }

        if (!habilitado) {
            return;
        }

        int indice = evento.getActionIndex();

        switch (acao) {

            case MotionEvent.ACTION_DOWN:

            case MotionEvent.ACTION_POINTER_DOWN:

                atribuirPonteiro(
                        view,
                        evento,
                        indice
                );

                if (modoSolicitacao) {
                    iniciarSolicitacaoSeNecessario();
                } else {
                    verificarControleManual();
                    atualizarControleManual();
                }

                break;

            case MotionEvent.ACTION_MOVE:

                atualizarPosicoesDedos(evento);

                if (!modoSolicitacao) {
                    atualizarControleManual();
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:

                atualizarPosicoesDedos(evento);

                int id = evento.getPointerId(indice);

                liberarPonteiro(id);

                finalizarToque();

                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:

                liberarTodosOsDedos();

                finalizarToque();
                break;
        }
    }

    private void iniciarSolicitacaoSeNecessario() {
        if (!esquerdoPressionado || !direitoPressionado ||
                solicitacaoEmAndamento) {
            return;
        }

        solicitacaoEmAndamento = true;
        solicitacaoConcluida = false;
        listener.onSolicitacaoTrocaIniciada();
        handler.postDelayed(
                concluirSolicitacao,
                DURACAO_TROCA_CONTROLE_MS
        );
    }

    private void finalizarToque() {
        if (modoSolicitacao) {
            if (!esquerdoPressionado || !direitoPressionado) {
                boolean estavaAguardando = solicitacaoEmAndamento;
                handler.removeCallbacks(concluirSolicitacao);
                solicitacaoEmAndamento = false;

                if (estavaAguardando && !solicitacaoConcluida) {
                    listener.onSolicitacaoTrocaCancelada();
                }
            }
            return;
        }
        verificarLiberacao();
    }

    // =====================================================
    // COORDENADAS NA TELA
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
    // IDENTIFICAÇÃO DO CONTROLE
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

            Log.d(TAG, "Dedo no controle esquerdo. ID: " + id);

        } else if (controle == 2 &&
                idDedoDireito == -1) {

            idDedoDireito = id;

            direitoPressionado = true;
            posicaoDedoDireito = y;

            Log.d(TAG, "Dedo no controle direito. ID: " + id);
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

            Log.d(TAG, "Controle esquerdo liberado.");
        }

        if (id == idDedoDireito) {

            idDedoDireito = -1;

            direitoPressionado = false;

            Log.d(TAG, "Controle direito liberado.");
        }
    }

    // =====================================================
    // LIBERA TODOS OS DEDOS
    // =====================================================

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

        if (!esquerdoPressionado ||
                !direitoPressionado) {

            return;
        }

        if (!listener.possuiGif()) {
            return;
        }

        if (!controleManualAtivo) {

            cancelarRetornoControles();
            controleManualAtivo = true;

            Log.d(TAG, "CONTROLE MANUAL INICIADO.");

            listener.onControleManualIniciado();
        }
    }

    // =====================================================
    // ATUALIZA POSIÇÃO DOS CONTROLES
    // =====================================================

    private void atualizarControleManual() {

        if (!controleManualAtivo ||
                !esquerdoPressionado ||
                !direitoPressionado ||
                !listener.possuiGif()) {

            return;
        }

        int altura = areaGif.getHeight();

        if (altura <= 0) return;

        int[] localizacao = new int[2];

        areaGif.getLocationOnScreen(localizacao);

        float posicaoMediaDedos =
                (posicaoDedoEsquerdo + posicaoDedoDireito) / 2f;
        float posicaoLocal =
                posicaoMediaDedos - localizacao[1];

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
        progressoVisualControles = progresso;

        int centroY = (int) posicaoLocal;

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

        listener.onProgressoManual(progresso);
    }

    // =====================================================
    // MOVIMENTAÇÃO DOS BOTÕES
    // =====================================================

    public void moverControles(float progresso) {

        if (modoSolicitacao) {
            return;
        }

        cancelarRetornoControles();
        progressoVisualControles = Math.max(
                0f,
                Math.min(1f, progresso)
        );
        moverControlesInterno(progressoVisualControles);
    }

    public void retornarControlesAoCentro() {
        if (modoSolicitacao) {
            return;
        }

        cancelarRetornoControles();
        float progressoInicial = progressoVisualControles;
        if (Math.abs(progressoInicial - 0.5f) < 0.001f) {
            progressoVisualControles = 0.5f;
            moverControlesInterno(0.5f);
            return;
        }

        animacaoRetornoControles = ValueAnimator.ofFloat(
                progressoInicial,
                0.5f
        );
        animacaoRetornoControles.setDuration(
                DURACAO_RETORNO_CONTROLES_MS
        );
        animacaoRetornoControles.setInterpolator(
                new LinearInterpolator()
        );
        animacaoRetornoControles.addUpdateListener(animator -> {
            progressoVisualControles =
                    (float) animator.getAnimatedValue();
            moverControlesInterno(progressoVisualControles);
        });
        animacaoRetornoControles.start();
    }

    private void centralizarControles() {
        cancelarRetornoControles();
        progressoVisualControles = 0.5f;
        areaGif.post(() -> {
            if (areaGif.getHeight() > 0 &&
                    controleEsquerdo.getHeight() > 0) {
                moverControlesInterno(0.5f);
                return;
            }

            View.OnLayoutChangeListener[] listener =
                    new View.OnLayoutChangeListener[1];
            listener[0] = (view, left, top, right, bottom,
                           oldLeft, oldTop, oldRight, oldBottom) -> {
                if (areaGif.getHeight() <= 0 ||
                        controleEsquerdo.getHeight() <= 0) {
                    return;
                }
                areaGif.removeOnLayoutChangeListener(listener[0]);
                moverControlesInterno(0.5f);
            };
            areaGif.addOnLayoutChangeListener(listener[0]);
        });
    }

    private void cancelarRetornoControles() {
        if (animacaoRetornoControles != null) {
            animacaoRetornoControles.cancel();
            animacaoRetornoControles = null;
        }
    }

    private void moverControlesInterno(float progressoFinal) {

        areaGif.post(() -> {

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
        });
    }

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

        if (esquerdoPressionado &&
                direitoPressionado) {
            atualizarControleManual();
            return;
        }

        boolean estavaAtivo =
                controleManualAtivo;
        controleManualAtivo = false;

        if (estavaAtivo) {

            Log.d(TAG, "CONTROLE MANUAL ENCERRADO.");

            retornarControlesAoCentro();
            listener.onControleManualLiberado();
        }
    }

    // =====================================================
    // CONSULTA DE ESTADO
    // =====================================================

    public boolean isEsquerdoPressionado() {
        return esquerdoPressionado;
    }

    public boolean isDireitoPressionado() {
        return direitoPressionado;
    }

    public boolean isControleManualAtivo() {
        return controleManualAtivo;
    }

    public void definirHabilitado(boolean habilitado) {
        this.habilitado = habilitado;

        if (!habilitado) {
            handler.removeCallbacks(concluirSolicitacao);
            solicitacaoEmAndamento = false;
            liberarTodosOsDedos();
            verificarLiberacao();
        }
    }

    public void definirModoSolicitacao(boolean modoSolicitacao) {
        if (this.modoSolicitacao == modoSolicitacao) {
            return;
        }

        handler.removeCallbacks(concluirSolicitacao);
        liberarTodosOsDedos();
        solicitacaoEmAndamento = false;
        if (!aguardandoFimSolicitacao) {
            solicitacaoConcluida = false;
        }
        controleManualAtivo = false;
        this.modoSolicitacao = modoSolicitacao;
    }

    public boolean isSolicitacaoTrocaEmAndamento() {
        return solicitacaoEmAndamento;
    }

    public boolean isAguardandoFimSolicitacao() {
        return aguardandoFimSolicitacao;
    }

    // =====================================================
    // LIMPEZA
    // =====================================================

    public void limpar() {

        cancelarRetornoControles();
        handler.removeCallbacks(concluirSolicitacao);
        liberarTodosOsDedos();

        controleManualAtivo = false;
        solicitacaoEmAndamento = false;
        solicitacaoConcluida = false;
        aguardandoFimSolicitacao = false;
    }
}