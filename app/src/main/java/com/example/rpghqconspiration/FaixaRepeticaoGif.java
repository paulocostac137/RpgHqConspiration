package com.example.rpghqconspiration;

/** Inclusive, one-based frame range marked for repeated playback. */
public final class FaixaRepeticaoGif {

    private final int primeiroQuadro;
    private final int ultimoQuadro;

    public FaixaRepeticaoGif(int primeiroQuadro, int ultimoQuadro) {
        if (primeiroQuadro <= 0 || ultimoQuadro < primeiroQuadro) {
            throw new IllegalArgumentException(
                    "A faixa Go1–Out1 precisa conter quadros válidos em ordem."
            );
        }
        this.primeiroQuadro = primeiroQuadro;
        this.ultimoQuadro = ultimoQuadro;
    }

    public int getPrimeiroQuadro() {
        return primeiroQuadro;
    }

    public int getUltimoQuadro() {
        return ultimoQuadro;
    }
}
