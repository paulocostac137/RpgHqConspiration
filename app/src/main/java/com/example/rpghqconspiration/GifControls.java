package com.example.rpghqconspiration;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
                manualAtivo = esquerdoPressionado && direitoPressionado;
                break;

            case MotionEvent.ACTION_MOVE:
                atualizarPosicoesDedos(evento);
                manualAtivo = esquerdoPressionado && direitoPressionado;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                atualizarPosicoesDedos(evento);
                liberarPonteiro(evento.getPointerId(indice));
                manualAtivo = esquerdoPressionado && direitoPressionado;
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
        int altura = areaGif.getHeight();
        if (altura <= 0) {
            return;
        }

        int[] localizacao = new int[2];
        areaGif.getLocationOnScreen(localizacao);

        float progressoEsquerdo = 0f;
        float progressoDireito = 0f;

        if (esquerdoPressionado) {
            float alturaBotao = controleEsquerdo.getHeight();
            if (alturaBotao <= 0 || altura <= alturaBotao) {
                return;
            }

            float posicaoLocal = limitarPosicao(
                    posicaoDedoEsquerdo - localizacao[1],
                    alturaBotao / 2f,
                    altura - alturaBotao / 2f
            );
            moverBotao(controleEsquerdo, (int) posicaoLocal);
            progressoEsquerdo = (posicaoLocal - alturaBotao / 2f) /
                    (altura - alturaBotao);
        }

        if (direitoPressionado) {
            float alturaBotao = controleDireito.getHeight();
            if (alturaBotao <= 0 || altura <= alturaBotao) {
                return;
            }

            float posicaoLocal = limitarPosicao(
                    posicaoDedoDireito - localizacao[1],
                    alturaBotao / 2f,
                    altura - alturaBotao / 2f
            );
            moverBotao(controleDireito, (int) posicaoLocal);
            progressoDireito = (posicaoLocal - alturaBotao / 2f) /
                    (altura - alturaBotao);
        }

        if (manualAtivo && onManualProgressChanged != null) {
            float progresso = (progressoEsquerdo + progressoDireito) / 2f;
            onManualProgressChanged.onProgressChanged(progresso);
        }
    }

    public void moverParaCentro() {
        int centro = areaGif.getHeight() / 2;
        moverBotao(controleEsquerdo, centro);
        moverBotao(controleDireito, centro);
    }

    private float limitarPosicao(float posicao, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, posicao));
    }

    private void moverBotao(TextView botao,
                            int centroY) {
        if (botao == null || botao.getHeight() <= 0) {
            return;
        }

        float centroInicial = botao.getTop() + botao.getHeight() / 2f;
        botao.setTranslationY(centroY - centroInicial);
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

        manualAtivo = esquerdoPressionado && direitoPressionado;
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
