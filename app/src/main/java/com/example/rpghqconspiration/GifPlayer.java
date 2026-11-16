package com.example.rpghqconspiration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;

public final class GifPlayer implements GifRenderer {

    private final Handler handler;
    private final ImageView imagemGif;
    private final View areaGif;

    private Movie movie;
    private Bitmap quadroBitmap;
    private Canvas quadroCanvas;
    private final Paint pincel = new Paint(
            Paint.ANTI_ALIAS_FLAG |
                    Paint.FILTER_BITMAP_FLAG
    );

    private long posicaoMs = 0;
    private long inicioReproducao = 0;
    private boolean reproduzindo = false;

    private final Runnable atualizadorGif = new Runnable() {
        @Override
        public void run() {
            if (movie == null || !reproduzindo) {
                return;
            }

            int duracao = movie.duration();
            if (duracao <= 0) {
                duracao = 1000;
            }

            long agora = SystemClock.uptimeMillis();
            long decorrido = agora - inicioReproducao;

            posicaoMs = (posicaoMs + decorrido) % duracao;
            inicioReproducao = agora;

            desenharQuadro();
            handler.postDelayed(this, 30);
        }
    };

    public GifPlayer(Handler handler,
                     ImageView imagemGif,
                     View areaGif) {
        this.handler = handler;
        this.imagemGif = imagemGif;
        this.areaGif = areaGif;
    }

    @Override
    public void exibir(File gifFile) throws Exception {
        carregar(gifFile);
    }

    public void carregar(File arquivo) throws Exception {
        handler.removeCallbacks(atualizadorGif);
        reproduzindo = false;
        movie = null;
        posicaoMs = 0;

        try (FileInputStream entrada = new FileInputStream(arquivo)) {
            Movie novoMovie = Movie.decodeStream(entrada);
            if (novoMovie == null) {
                throw new Exception("Não foi possível decodificar o GIF.");
            }

            movie = novoMovie;
            desenharQuadro();
            iniciarAnimacao();
        }
    }

    public void pausar() {
        if (movie == null) return;

        if (reproduzindo) {
            int duracao = movie.duration();
            if (duracao <= 0) {
                duracao = 1000;
            }
            long decorrido = SystemClock.uptimeMillis() - inicioReproducao;
            posicaoMs = (posicaoMs + decorrido) % duracao;
        }

        reproduzindo = false;
        handler.removeCallbacks(atualizadorGif);
    }

    public void iniciarAnimacao() {
        if (movie == null || reproduzindo) {
            return;
        }

        reproduzindo = true;
        inicioReproducao = SystemClock.uptimeMillis();
        handler.removeCallbacks(atualizadorGif);
        handler.post(atualizadorGif);
    }

    public void setPosicaoMs(long posicaoMs) {
        this.posicaoMs = posicaoMs;
        desenharQuadro();
    }

    public long getPosicaoMs() {
        return posicaoMs;
    }

    public void resetar() {
        pausar();
        posicaoMs = 0;
        desenharQuadro();
    }

    public Movie getMovie() {
        return movie;
    }

    public void limpar() {
        pausar();
        handler.removeCallbacks(atualizadorGif);

        if (quadroBitmap != null) {
            quadroBitmap.recycle();
            quadroBitmap = null;
            quadroCanvas = null;
        }

        movie = null;
        posicaoMs = 0;
        inicioReproducao = 0;
        reproduzindo = false;
    }

    public void desenharQuadro() {
        if (movie == null || areaGif.getWidth() <= 0 || areaGif.getHeight() <= 0) {
            return;
        }

        int largura = areaGif.getWidth();
        int altura = areaGif.getHeight();

        if (quadroBitmap == null ||
                quadroBitmap.getWidth() != largura ||
                quadroBitmap.getHeight() != altura) {

            if (quadroBitmap != null) {
                quadroBitmap.recycle();
            }

            quadroBitmap = Bitmap.createBitmap(
                    largura,
                    altura,
                    Bitmap.Config.ARGB_8888
            );
            quadroCanvas = new Canvas(quadroBitmap);
            imagemGif.setImageBitmap(quadroBitmap);
        }

        if (quadroCanvas == null) {
            return;
        }

        quadroCanvas.drawColor(Color.BLACK);

        int larguraGif = movie.width();
        int alturaGif = movie.height();

        if (larguraGif <= 0 || alturaGif <= 0) {
            return;
        }

        float escala = Math.min(
                (float) largura / larguraGif,
                (float) altura / alturaGif
        );

        float larguraFinal = larguraGif * escala;
        float alturaFinal = alturaGif * escala;

        float deslocamentoX = (largura - larguraFinal) / 2f;
        float deslocamentoY = (altura - alturaFinal) / 2f;

        quadroCanvas.save();
        quadroCanvas.translate(deslocamentoX, deslocamentoY);
        quadroCanvas.scale(escala, escala);

        movie.setTime((int) posicaoMs);
        movie.draw(quadroCanvas, 0, 0, pincel);

        quadroCanvas.restore();
        imagemGif.invalidate();
    }
}
