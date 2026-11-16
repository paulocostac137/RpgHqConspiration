package com.example.rpghqconspiration;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class GifControls {

    private final View areaGif;
    private final TextView controleEsquerdo;
    private final TextView controleDireito;

    private boolean esquerdoPressionado = false;
    private boolean direitoPressionado = false;
    private boolean manualAtivo = false;

    private float posicaoDedoEsquerdo = 0f;
    private float posicaoDedoDireito = 0f;
    private int idDedoEsquerdo = -1;
    private int idDedoDireito = -1;

    public GifControls(View areaGif,
                       TextView controleEsquerdo,
                       TextView controleDireito) {
        this.areaGif = areaGif;
        this.controleEsquerdo = controleEsquerdo;
        this.controleDireito = controleDireito;
    }

    public void configurar() {
        if (areaGif instanceof ViewGroup) {
            ((ViewGroup) areaGif).setMotionEventSplittingEnabled(false);
        }
    }

    public void processarEventoMultitoque(View view, MotionEvent evento) {
        int acao = evento.getActionMasked();
        int indice = evento.getActionIndex();

        switch (acao) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                atribuirPonteiro(view, evento, indice);
                manualAtivo = esquerdoPressionado || direitoPressionado;
                break;

            case MotionEvent.ACTION_MOVE:
                atualizarPosicoesDedos(evento);
                manualAtivo = esquerdoPressionado || direitoPressionado;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                atualizarPosicoesDedos(evento);
                liberarPonteiro(evento.getPointerId(indice));
                manualAtivo = esquerdoPressionado || direitoPressionado;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                liberarTodosOsDedos();
                manualAtivo = false;
                break;
        }
    }

    public boolean isManualAtivo() {
        return manualAtivo;
    }

    public boolean isEsquerdoPressionado() {
        return esquerdoPressionado;
    }

    public boolean isDireitoPressionado() {
        return direitoPressionado;
    }

    public float getPosicaoDedoEsquerdo() {
        return posicaoDedoEsquerdo;
    }

    public float getPosicaoDedoDireito() {
        return posicaoDedoDireito;
    }

    public void resetar() {
        idDedoEsquerdo = -1;
        idDedoDireito = -1;
        esquerdoPressionado = false;
        direitoPressionado = false;
        manualAtivo = false;
    }

    public void iniciarManual() {
        if (!esquerdoPressionado && !direitoPressionado) {
            return;
        }
        manualAtivo = true;
    }

    public void encerrarManual() {
        manualAtivo = false;
    }

    public void atualizarPosicoesDedo() {
        if (!manualAtivo) {
            return;
        }

        int altura = areaGif.getHeight();
        if (altura <= 0) {
            return;
        }

        int[] localizacao = new int[2];
        areaGif.getLocationOnScreen(localizacao);

        float posicaoMedia;

        if (esquerdoPressionado && direitoPressionado) {
            posicaoMedia = (posicaoDedoEsquerdo + posicaoDedoDireito) / 2f;
        } else if (esquerdoPressionado) {
            posicaoMedia = posicaoDedoEsquerdo;
        } else {
            posicaoMedia = posicaoDedoDireito;
        }

        float posicaoLocal = posicaoMedia - localizacao[1];
        float metadeBotao = controleEsquerdo.getHeight() / 2f;
        float limiteInferior = metadeBotao;
        float limiteSuperior = altura - metadeBotao;

        if (limiteSuperior <= limiteInferior) {
            return;
        }

        posicaoLocal = Math.max(limiteInferior,
                Math.min(limiteSuperior, posicaoLocal));

        float progresso = (posicaoLocal - metadeBotao) /
                (altura - 2f * metadeBotao);

        progresso = Math.max(0f, Math.min(1f, progresso));

        int centroY = (int) posicaoLocal;

        if (esquerdoPressionado && direitoPressionado) {
            moverBotao(controleEsquerdo, centroY, true);
            moverBotao(controleDireito, centroY, false);
        } else if (esquerdoPressionado) {
            moverBotao(controleEsquerdo, centroY, true);
        } else {
            moverBotao(controleDireito, centroY, false);
        }

        if (onManualProgressChanged != null) {
            onManualProgressChanged.onProgressChanged(progresso);
        }
    }

    public void moverParaCentro() {
        int centro = areaGif.getHeight() / 2;
        moverBotao(controleEsquerdo, centro, true);
        moverBotao(controleDireito, centro, false);
    }

    private void moverBotao(TextView botao,
                            int centroY,
                            boolean esquerdo) {
        if (botao == null || botao.getLayoutParams() == null) {
            return;
        }

        FrameLayout.LayoutParams parametros =
                (FrameLayout.LayoutParams) botao.getLayoutParams();

        parametros.gravity = Gravity.TOP |
                (esquerdo ? Gravity.START : Gravity.END);
        parametros.topMargin = Math.max(0,
                centroY - botao.getHeight() / 2);

        botao.setLayoutParams(parametros);
    }

    private int identificarControle(float x, float y) {
        int[] localizacao = new int[2];

        controleEsquerdo.getLocationOnScreen(localizacao);
        if (x >= localizacao[0] &&
                x <= localizacao[0] + controleEsquerdo.getWidth() &&
                y >= localizacao[1] &&
                y <= localizacao[1] + controleEsquerdo.getHeight()) {
            return 1;
        }

        controleDireito.getLocationOnScreen(localizacao);
        if (x >= localizacao[0] &&
                x <= localizacao[0] + controleDireito.getWidth() &&
                y >= localizacao[1] &&
                y <= localizacao[1] + controleDireito.getHeight()) {
            return 2;
        }

        return 0;
    }

    private float obterXNaTela(MotionEvent evento, int indice) {
        float deslocamentoX = evento.getRawX() - evento.getX(0);
        return evento.getX(indice) + deslocamentoX;
    }

    private float obterYNaTela(MotionEvent evento, int indice) {
        float deslocamentoY = evento.getRawY() - evento.getY(0);
        return evento.getY(indice) + deslocamentoY;
    }

    private void atribuirPonteiro(View view,
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
        } else if (controle == 2 && idDedoDireito == -1) {
            idDedoDireito = id;
            direitoPressionado = true;
            posicaoDedoDireito = y;
        }
    }

    private void atualizarPosicoesDedos(MotionEvent evento) {
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

    private void liberarPonteiro(int id) {
        if (id == idDedoEsquerdo) {
            idDedoEsquerdo = -1;
            esquerdoPressionado = false;
        }

        if (id == idDedoDireito) {
            idDedoDireito = -1;
            direitoPressionado = false;
        }

        manualAtivo = esquerdoPressionado || direitoPressionado;
    }

    private void liberarTodosOsDedos() {
        idDedoEsquerdo = -1;
        idDedoDireito = -1;
        esquerdoPressionado = false;
        direitoPressionado = false;
        manualAtivo = false;
    }

    public void setOnManualProgressChanged(ManualProgressChanged callback) {
        this.onManualProgressChanged = callback;
    }

    public interface ManualProgressChanged {
        void onProgressChanged(float progresso);
    }

    private ManualProgressChanged onManualProgressChanged;
}
