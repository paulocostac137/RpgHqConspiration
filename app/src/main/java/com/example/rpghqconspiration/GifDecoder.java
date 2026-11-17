package com.example.rpghqconspiration;

import android.graphics.Movie;

import java.io.File;
import java.io.FileInputStream;

/**
 * Responsável por abrir e decodificar arquivos GIF.
 *
 * Não manipula Views nem controla a animação.
 */
public final class GifDecoder {

    private GifDecoder() {
        // Impede a criação de instâncias.
    }

    /**
     * Abre um arquivo GIF e retorna seu objeto Movie.
     */
    public static Movie carregar(File arquivo) throws Exception {

        if (arquivo == null || !arquivo.exists()) {
            throw new Exception("Arquivo GIF não encontrado.");
        }

        if (arquivo.length() == 0) {
            throw new Exception("Arquivo GIF vazio.");
        }

        try (
                FileInputStream entrada =
                        new FileInputStream(arquivo)
        ) {

            Movie movie = Movie.decodeStream(entrada);

            if (movie == null) {
                throw new Exception(
                        "Não foi possível decodificar o GIF."
                );
            }

            return movie;
        }
    }
}