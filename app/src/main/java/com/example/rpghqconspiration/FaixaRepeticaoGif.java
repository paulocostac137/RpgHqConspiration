package com.example.rpghqconspiration;

/** Inclusive, one-based frame range marked for repeated playback. */
public final class FaixaRepeticaoGif {

    private final int primeiroQuadro;
    private final int quadroOn;
    private final int ultimoQuadro;

    public FaixaRepeticaoGif(int primeiroQuadro, int ultimoQuadro) {
        this(primeiroQuadro, ultimoQuadro, ultimoQuadro);
    }

    public FaixaRepeticaoGif(
            int primeiroQuadro,
            int quadroOn,
            int ultimoQuadro) {
        if (primeiroQuadro <= 0 ||
                quadroOn < primeiroQuadro ||
                ultimoQuadro < quadroOn) {
            throw new IllegalArgumentException(
                    "A faixa Go1–On1–Out1 precisa conter quadros válidos em ordem."
            );
        }
        this.primeiroQuadro = primeiroQuadro;
        this.quadroOn = quadroOn;
        this.ultimoQuadro = ultimoQuadro;
    }

    public int getPrimeiroQuadro() {
        return primeiroQuadro;
    }

    public int getUltimoQuadro() {
        return ultimoQuadro;
    }

    public int getQuadroOn() {
        return quadroOn;
    }
}
