package com.example.rpghqconspiration;

import java.util.ArrayList;

/**
 * Responsável por interpretar os registros
 * recebidos da planilha Google Sheets.
 *
 * Não realiza conexões HTTP e não manipula Views.
 */
public final class PlanilhaParser {

    private PlanilhaParser() {
        // Impede a criação de instâncias.
    }

    /**
     * Interpreta o conteúdo CSV e preenche
     * as listas utilizadas pela MainActivity.
     */
    public static void preencherListas(
            String conteudo,
            ArrayList<String> nomes,
            ArrayList<String> links,
            ArrayList<String> textosPlanilha) {

        nomes.clear();
        links.clear();
        textosPlanilha.clear();

        String[] linhas = conteudo.split("\\n");

        boolean primeiraLinha = true;

        for (String linha : linhas) {

            ArrayList<String> colunas =
                    CsvParser.separar(linha);

            if (primeiraLinha) {

                primeiraLinha = false;

                continue;
            }

            if (colunas.size() < 2) {
                continue;
            }

            String nome =
                    colunas.get(0).trim();

            String link =
                    colunas.get(1).trim();

            String texto =
                    colunas.size() > 4
                            ? colunas.get(4).trim()
                            : "";

            if (!nome.isEmpty() &&
                    !link.isEmpty()) {

                nomes.add(nome);
                links.add(link);
                textosPlanilha.add(texto);
            }
        }
    }
}