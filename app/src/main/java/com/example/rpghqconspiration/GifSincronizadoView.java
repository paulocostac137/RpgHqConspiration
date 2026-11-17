package com.example.rpghqconspiration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import java.io.File;
import java.io.FileInputStream;

public class GifSincronizadoView extends AppCompatImageView {

    private Movie movie;

    private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG |
                    Paint.FILTER_BITMAP_FLAG
    );

    private int duracaoMs = 1000;
    private int posicaoMs = 0;
    private boolean reproduzindo = false;

    public GifSincronizadoView(Context context) {
        super(context);
        inicializar();
    }

    public GifSincronizadoView(
            Context context,
            AttributeSet attrs
    ) {
        super(context, attrs);
        inicializar();
    }

    public GifSincronizadoView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        inicializar();
    }

    private void inicializar() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.BLACK);
        setScaleType(ScaleType.FIT_CENTER);
    }

    public synchronized boolean carregarGif(File arquivo) {

        if (arquivo == null ||
                !arquivo.exists() ||
                arquivo.length() <= 0) {
            return false;
        }

        try (FileInputStream entrada =
                     new FileInputStream(arquivo)) {

            Movie novoMovie = Movie.decodeStream(entrada);

            if (novoMovie == null) {
                return false;
            }

            movie = novoMovie;

            int duracao = movie.duration();

            duracaoMs = duracao > 0 ? duracao : 1000;

            posicaoMs = 0;
            reproduzindo = false;

            postInvalidate();

            return true;

        } catch (Exception e) {

            movie = null;

            return false;
        }
    }

    public synchronized void definirPosicao(int posicao) {

        if (movie == null) return;

        posicaoMs = limitarPosicao(posicao);

        postInvalidate();
    }

    public synchronized void iniciar() {

        if (movie == null) return;

        reproduzindo = true;

        postInvalidate();
    }

    public synchronized void pausar() {

        reproduzindo = false;

        postInvalidate();
    }

    public synchronized void reiniciar() {

        posicaoMs = 0;

        reproduzindo = false;

        postInvalidate();
    }

    public synchronized int getDuracaoMs() {
        return duracaoMs;
    }

    public synchronized int getPosicaoMs() {
        return posicaoMs;
    }

    public synchronized boolean estaReproduzindo() {
        return reproduzindo;
    }

    public synchronized boolean possuiGif() {
        return movie != null;
    }

    private int limitarPosicao(int posicao) {

        return Math.max(
                0,
                Math.min(posicao, duracaoMs - 1)
        );
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {

        if (movie == null) {
            super.onDraw(canvas);
            return;
        }

        canvas.drawColor(Color.BLACK);

        int largura = getWidth();
        int altura = getHeight();

        if (largura <= 0 || altura <= 0) return;

        int larguraGif = movie.width();
        int alturaGif = movie.height();

        if (larguraGif <= 0 || alturaGif <= 0) return;

        movie.setTime(limitarPosicao(posicaoMs));

        float escala = Math.min(
                (float) largura / larguraGif,
                (float) altura / alturaGif
        );

        float x = (largura - larguraGif * escala) / 2f;
        float y = (altura - alturaGif * escala) / 2f;

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(escala, escala);

        movie.draw(canvas, 0, 0, paint);

        canvas.restore();
    }
}