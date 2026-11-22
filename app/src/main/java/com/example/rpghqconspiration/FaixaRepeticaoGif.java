package com.example.rpghqconspiration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Inclusive, one-based frame range marked for repeated playback. */
public final class FaixaRepeticaoGif {

    private final int primeiroQuadro;
    private final int quadroOn;
    private final int ultimoQuadro;
    private final List<FaixaRepeticaoGif> faixas;

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
        this.faixas = Collections.singletonList(this);
    }

    private FaixaRepeticaoGif(List<FaixaRepeticaoGif> faixas) {
        if (faixas == null || faixas.isEmpty()) {
            throw new IllegalArgumentException(
                    "A lista de faixas marcadas não pode estar vazia."
            );
        }
        ArrayList<FaixaRepeticaoGif> copia =
                new ArrayList<>(faixas.size());
        int ultimoQuadroAnterior = 0;
        for (FaixaRepeticaoGif faixa : faixas) {
            if (faixa == null || faixa.faixas.size() != 1 ||
                    faixa.primeiroQuadro < ultimoQuadroAnterior) {
                throw new IllegalArgumentException(
                        "As faixas marcadas precisam estar em ordem."
                );
            }
            copia.add(faixa);
            ultimoQuadroAnterior = faixa.ultimoQuadro;
        }
        FaixaRepeticaoGif primeira = copia.get(0);
        this.primeiroQuadro = primeira.primeiroQuadro;
        this.quadroOn = primeira.quadroOn;
        this.ultimoQuadro = primeira.ultimoQuadro;
        this.faixas = Collections.unmodifiableList(copia);
    }

    public static FaixaRepeticaoGif agrupar(
            List<FaixaRepeticaoGif> faixas) {
        if (faixas == null || faixas.isEmpty()) {
            return null;
        }
        if (faixas.size() == 1) {
            return faixas.get(0);
        }
        return new FaixaRepeticaoGif(faixas);
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

    public List<FaixaRepeticaoGif> getFaixas() {
        return faixas;
    }
}
