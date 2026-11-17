package com.example.rpghqconspiration;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Gerencia a apresentação temporizada do texto
 * associado ao GIF atual.
 *
 * Não consulta a planilha nem controla a animação.
 */
public final class GerenciadorTextoPlanilha {

    private static final long JANELA_TEXTO_MS = 7000;
    private static final long TEMPO_TEXTO_MS = 5000;
    private static final long INTERVALO_MS = 50;

    public interface Listener {
        boolean atividadeDisponivel();
        boolean possuiGif();
    }

    private final TextView texto;
    private final FrameLayout areaGif;
    private final Handler handler;
    private final Listener listener;

    private long inicioExibicao = 0;

    private boolean ativo = false;
    private boolean encerrado = false;
    private boolean temporizadorExterno = false;

    private final Runnable atualizar = new Runnable() {
        @Override
        public void run() {

            if (encerrado || !ativo || temporizadorExterno) {
                return;
            }

            if (!listener.atividadeDisponivel() ||
                    !listener.possuiGif()) {

                parar();
                return;
            }

            long decorrido =
                    SystemClock.uptimeMillis() - inicioExibicao;

            if (decorrido >= TEMPO_TEXTO_MS &&
                    decorrido < JANELA_TEXTO_MS) {

                texto.setVisibility(View.VISIBLE);

            } else {

                texto.setVisibility(View.GONE);
            }

            if (decorrido < JANELA_TEXTO_MS) {

                handler.postDelayed(
                        this,
                        INTERVALO_MS
                );

            } else {

                ativo = false;
                texto.setVisibility(View.GONE);
            }
        }
    };

    public GerenciadorTextoPlanilha(
            TextView texto,
            FrameLayout areaGif,
            Handler handler,
            Listener listener) {

        if (texto == null ||
                areaGif == null ||
                handler == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "Os componentes e o Listener são obrigatórios."
            );
        }

        this.texto = texto;
        this.areaGif = areaGif;
        this.handler = handler;
        this.listener = listener;
    }

    /**
     * Configura aparência e posicionamento.
     */
    public void configurar() {

        texto.setBackground(
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

        texto.setVisibility(View.GONE);

        areaGif.post(() -> {

            if (encerrado ||
                    !listener.atividadeDisponivel()) {
                return;
            }

            int altura = areaGif.getHeight();

            if (altura <= 0) {
                return;
            }

            FrameLayout.LayoutParams parametros =
                    (FrameLayout.LayoutParams)
                            texto.getLayoutParams();

            parametros.width =
                    FrameLayout.LayoutParams.MATCH_PARENT;

            parametros.height = altura / 4;

            parametros.gravity = Gravity.TOP;

            texto.setLayoutParams(parametros);
        });
    }

    /**
     * Inicia a apresentação do texto para o GIF atual.
     */
    public void iniciar(String conteudo) {

        if (encerrado ||
                !listener.atividadeDisponivel() ||
                !listener.possuiGif()) {
            return;
        }

        parar();

        texto.setText(conteudo == null ? "" : conteudo);

        texto.setVisibility(View.GONE);

        inicioExibicao = SystemClock.uptimeMillis();

        ativo = true;

        handler.post(atualizar);
    }

    public void iniciarComVisibilidadeExterna(String conteudo) {
        if (encerrado ||
                !listener.atividadeDisponivel() ||
                !listener.possuiGif()) {
            return;
        }

        parar();
        temporizadorExterno = true;
        texto.setText(conteudo == null ? "" : conteudo);
        texto.setVisibility(View.GONE);
        ativo = true;
    }

    public void definirVisivel(boolean visivel) {
        if (encerrado || !ativo || !temporizadorExterno) {
            return;
        }
        texto.setVisibility(visivel ? View.VISIBLE : View.GONE);
    }

    /**
     * Interrompe a apresentação e oculta o texto.
     */
    public void parar() {

        ativo = false;
        temporizadorExterno = false;

        inicioExibicao = 0;

        handler.removeCallbacks(atualizar);

        texto.setVisibility(View.GONE);
    }

    /**
     * Libera o gerenciador definitivamente.
     */
    public void encerrar() {

        parar();

        encerrado = true;
    }
}