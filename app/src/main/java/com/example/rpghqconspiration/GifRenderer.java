package com.example.rpghqconspiration;

import android.graphics.Movie;

import java.io.File;

public interface GifRenderer {
    void exibir(File gifFile) throws Exception;
    void pausar();
    void iniciarAnimacao();
    void setPosicaoMs(long posicaoMs);
    Movie getMovie();
    void desenharQuadro();
}
