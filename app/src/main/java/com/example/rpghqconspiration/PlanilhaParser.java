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
        preencherListas(
                conteudo,
                nomes,
                links,
                textosPlanilha,
                new ArrayList<>()
        );
    }

    public static void preencherListas(
            String conteudo,
            ArrayList<String> nomes,
            ArrayList<String> links,
            ArrayList<String> textosPlanilha,
            ArrayList<Integer> repeticoes) {

        nomes.clear();
        links.clear();
        textosPlanilha.clear();
        repeticoes.clear();

        String[] linhas = conteudo.split("\\n");

        boolean primeiraLinha = true;
        int numeroLinha = 0;

        for (String linha : linhas) {
            numeroLinha++;

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
                    colunas.size() > 2
                            ? colunas.get(2).trim()
                            : "";
            String repeticaoTexto =
                    colunas.size() > 3
                            ? colunas.get(3).trim()
                            : "";

            if (!nome.isEmpty() &&
                    !link.isEmpty()) {

                int quantidadeRepeticoes = 1;
                if (!repeticaoTexto.isEmpty()) {
                    try {
                        quantidadeRepeticoes =
                                Integer.parseInt(repeticaoTexto);
                    } catch (NumberFormatException erro) {
                        throw new IllegalArgumentException(
                                "A coluna D da linha " + numeroLinha +
                                        " deve conter um número inteiro positivo.",
                                erro
                        );
                    }
                    if (quantidadeRepeticoes <= 0) {
                        throw new IllegalArgumentException(
                                "A coluna D da linha " + numeroLinha +
                                        " deve conter um número inteiro positivo."
                        );
                    }
                }

                nomes.add(nome);
                links.add(link);
                textosPlanilha.add(texto);
                repeticoes.add(quantidadeRepeticoes);
            }
        }
    }
}