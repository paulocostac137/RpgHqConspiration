package com.example.rpghqconspiration;

import java.util.List;

final class MarcadoresPlanilha {

    private static final int TOTAL_CICLOS = 3;
    private static final int MARCACOES_POR_CICLO = 3;

    private MarcadoresPlanilha() {
    }

    static String[] colunasPorCiclo(List<Integer> ordemMarcacoes) {
        StringBuilder[] colunas = new StringBuilder[
                TOTAL_CICLOS * MARCACOES_POR_CICLO
        ];
        for (int i = 0; i < colunas.length; i++) {
            colunas[i] = new StringBuilder();
        }

        for (int i = 0; i < ordemMarcacoes.size(); i++) {
            int indiceColuna = i % colunas.length;
            StringBuilder coluna = colunas[indiceColuna];
            if (coluna.length() > 0) {
                coluna.append(',');
            }
            coluna.append(ordemMarcacoes.get(i) + 1);
        }

        String[] resultado = new String[colunas.length];
        for (int i = 0; i < colunas.length; i++) {
            resultado[i] = colunas[i].toString();
        }
        return resultado;
    }
}
