package com.example.rpghqconspiration;

final class SequenciaMarcadores {

    private static final int TOTAL_MARCACOES = 9;
    private int totalMarcacoes;
    private int ultimoIndiceQuadro = -1;

    boolean podeAdicionar(char marcacao, int indiceQuadro) {
        if (totalMarcacoes >= TOTAL_MARCACOES ||
                marcacao != obterProximaMarcacao()) {
            return false;
        }
        if (totalMarcacoes == 0) {
            return true;
        }

        boolean inicioNovoCiclo =
                marcacao == 'G'
                        && (totalMarcacoes == 3
                        || totalMarcacoes == 6);
        if (inicioNovoCiclo) {
            return indiceQuadro >= ultimoIndiceQuadro;
        }
        return indiceQuadro > ultimoIndiceQuadro;
    }

    boolean adicionar(char marcacao, int indiceQuadro) {
        if (!podeAdicionar(marcacao, indiceQuadro)) {
            return false;
        }
        totalMarcacoes++;
        ultimoIndiceQuadro = indiceQuadro;
        return true;
    }

    char obterProximaMarcacao() {
        if (totalMarcacoes >= TOTAL_MARCACOES) {
            return '\0';
        }
        switch (totalMarcacoes % 3) {
            case 0:
                return 'G';
            case 1:
                return 'O';
            default:
                return 'T';
        }
    }

    boolean temPeloMenosTresMarcacoes() {
        return totalMarcacoes >= 3;
    }

    String obterRotuloOn() {
        int ciclo = Math.min(3, totalMarcacoes / 3 + 1);
        return ciclo == 1 ? "On" : "On" + ciclo;
    }
}
