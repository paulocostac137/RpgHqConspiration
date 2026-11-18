package com.example.rpghqconspiration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Responsável por desenhar os quadros dos GIFs.
 *
 * Controla o Bitmap e o Canvas utilizados
 * para apresentar a animação na ImageView.
 */
public class GifFrameRenderer {

    private final ImageView imagemGif;

    private Bitmap quadroBitmap;
    private Canvas quadroCanvas;

    public GifFrameRenderer(ImageView imagemGif) {
        this.imagemGif = imagemGif;
    }

    public void desenhar(
            Movie movie,
            long posicaoMs,
            Paint pincel) {

        if (movie == null ||
                imagemGif == null ||
                imagemGif.getWidth() <= 0 ||
                imagemGif.getHeight() <= 0) {

            return;
        }

        int largura = imagemGif.getWidth();
        int altura = imagemGif.getHeight();

        if (quadroBitmap == null ||
                quadroBitmap.getWidth() != largura ||
                quadroBitmap.getHeight() != altura) {

            liberarBitmap();

            quadroBitmap = Bitmap.createBitmap(
                    largura,
                    altura,
                    Bitmap.Config.ARGB_8888
            );

            quadroCanvas = new Canvas(quadroBitmap);

            imagemGif.setImageBitmap(quadroBitmap);
        } else {
            Drawable drawableAtual = imagemGif.getDrawable();
            if (!(drawableAtual instanceof BitmapDrawable) ||
                    ((BitmapDrawable) drawableAtual).getBitmap() != quadroBitmap) {
                imagemGif.setImageBitmap(quadroBitmap);
            }
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

        float deslocamentoX =
                (largura - larguraFinal) / 2f;

        float deslocamentoY =
                (altura - alturaFinal) / 2f;

        quadroCanvas.save();

        quadroCanvas.translate(
                deslocamentoX,
                deslocamentoY
        );

        quadroCanvas.scale(escala, escala);

        movie.setTime((int) posicaoMs);

        movie.draw(
                quadroCanvas,
                0,
                0,
                pincel
        );

        quadroCanvas.restore();

        imagemGif.invalidate();
    }

    public void liberar() {

        liberarBitmap();

        quadroCanvas = null;
    }

    private void liberarBitmap() {

        if (quadroBitmap != null) {

            if (!quadroBitmap.isRecycled()) {
                quadroBitmap.recycle();
            }

            quadroBitmap = null;
        }
    }
}