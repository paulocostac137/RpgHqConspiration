package com.example.rpghqconspiration;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class MainUiState {

    private final TextView status;
    private final TextView textoPlanilha;
    private final Button botaoProximo;
    private final FrameLayout areaGif;

    public MainUiState(TextView status,
                       TextView textoPlanilha,
                       Button botaoProximo,
                       FrameLayout areaGif) {
        this.status = status;
        this.textoPlanilha = textoPlanilha;
        this.botaoProximo = botaoProximo;
        this.areaGif = areaGif;
    }

    public void configurarTextoPlanilha() {
        if (textoPlanilha == null) return;

        textoPlanilha.setBackground(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{
                                Color.argb(250, 0, 0, 0),
                                Color.argb(220, 0, 0, 0),
                                Color.argb(140, 0, 0, 0),
                                Color.argb(0, 0, 0, 0)
                        }
                )
        );

        textoPlanilha.setVisibility(View.GONE);

        if (areaGif != null) {
            areaGif.post(() -> {
                int altura = areaGif.getHeight();
                if (altura <= 0) return;

                FrameLayout.LayoutParams parametros =
                        (FrameLayout.LayoutParams)
                                textoPlanilha.getLayoutParams();

                parametros.width = FrameLayout.LayoutParams.MATCH_PARENT;
                parametros.height = altura / 4;
                parametros.gravity = Gravity.TOP;
                textoPlanilha.setLayoutParams(parametros);
            });
        }
    }

    public void definirStatus(String mensagem) {
        if (status != null && mensagem != null) {
            status.setText(mensagem);
        }
    }

    public void definirBotaoProximo(boolean habilitado) {
        if (botaoProximo != null) {
            botaoProximo.setEnabled(habilitado);
        }
    }

    public void definirTextoPlanilha(String texto) {
        if (textoPlanilha != null) {
            textoPlanilha.setText(texto == null ? "" : texto);
        }
    }

    public void mostrarTextoPlanilha(boolean visivel) {
        if (textoPlanilha != null) {
            textoPlanilha.setVisibility(visivel ? View.VISIBLE : View.GONE);
        }
    }

    public void centralizarControles(TextView esquerdo, TextView direito) {
        if (areaGif == null || esquerdo == null || direito == null) {
            return;
        }

        areaGif.post(() -> {
            int centro = areaGif.getHeight() / 2;

            moverBotao(esquerdo, centro, true);
            moverBotao(direito, centro, false);
        });
    }

    private void moverBotao(TextView botao, int centroY, boolean esquerdo) {
        if (botao == null || botao.getLayoutParams() == null) {
            return;
        }

        FrameLayout.LayoutParams parametros =
                (FrameLayout.LayoutParams) botao.getLayoutParams();

        parametros.gravity = Gravity.TOP |
                (esquerdo ? Gravity.START : Gravity.END);

        parametros.topMargin = Math.max(
                0,
                centroY - botao.getHeight() / 2
        );

        botao.setLayoutParams(parametros);
    }
}
