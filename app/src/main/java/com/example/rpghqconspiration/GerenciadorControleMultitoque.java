package com.example.rpghqconspiration;

import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Responsável exclusivamente pelo gerenciamento
 * dos controles multitouch.
 *
 * Não controla GIFs, servidor ou reprodução.
 */
public final class GerenciadorControleMultitoque {

    private static final String TAG =
            "GerenciadorMultitoque";

    // =====================================================
    // COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        boolean possuiGif();

        void onControleManualIniciado();

        void onProgressoManual(float progresso);

        void onControleManualLiberado();
    }

    private final FrameLayout areaGif;

    private final TextView controleEsquerdo;

    private final TextView controleDireito;

    private final Listener listener;

    // =====================================================
    // ESTADO DOS DEDOS
    // =====================================================

    private boolean esquerdoPressionado = false;

    private boolean direitoPressionado = false;

    private boolean controleManualAtivo = false;

    private float posicaoDedoEsquerdo = 0;

    private float posicaoDedoDireito = 0;

    private int idDedoEsquerdo = -1;

    private int idDedoDireito = -1;

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

                int id = evento.getPointerId(indice);

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

        if (!esquerdoPressionado &&
                !direitoPressionado) {

            return;
        }

        if (!listener.possuiGif()) {
            return;
        }

        if (!controleManualAtivo) {

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
                (!esquerdoPressionado &&
                        !direitoPressionado) ||
                !listener.possuiGif()) {

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

        listener.onProgressoManual(progresso);
    }

    // =====================================================
    // MOVIMENTAÇÃO DOS BOTÕES
    // =====================================================

    public void moverControles(float progresso) {

        final float progressoFinal = Math.max(
                0f,
                Math.min(1f, progresso)
        );

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

        if (esquerdoPressionado ||
                direitoPressionado) {

            atualizarControleManual();

            return;
        }

        boolean estavaAtivo = controleManualAtivo;

        controleManualAtivo = false;

        if (estavaAtivo) {

            Log.d(TAG, "CONTROLE MANUAL ENCERRADO.");

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

    // =====================================================
    // LIMPEZA
    // =====================================================

    public void limpar() {

        liberarTodosOsDedos();

        controleManualAtivo = false;
    }
}