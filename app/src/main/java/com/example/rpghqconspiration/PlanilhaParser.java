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
        if (linhas.length == 0) {
            throw new IllegalArgumentException(
                    "A planilha está vazia."
            );
        }

        ArrayList<String> cabecalhos = CsvParser.separar(linhas[0]);
        int colunaNome = localizarColuna(cabecalhos, "Pov000");
        int colunaLink = localizarColuna(cabecalhos, "Link");
        int colunaTexto = localizarColuna(cabecalhos, "Express");
        localizarColuna(cabecalhos, "Go1");
        localizarColuna(cabecalhos, "On1");
        localizarColuna(cabecalhos, "Out1");

        for (int numeroLinha = 1; numeroLinha < linhas.length; numeroLinha++) {
            ArrayList<String> colunas =
                    CsvParser.separar(linhas[numeroLinha]);

            int ultimaColunaNecessaria = Math.max(
                    colunaNome,
                    Math.max(colunaLink, colunaTexto)
            );
            if (colunas.size() <= ultimaColunaNecessaria) {
                continue;
            }

            String nome =
                    colunas.get(colunaNome).trim();

            String link =
                    colunas.get(colunaLink).trim();

            String texto =
                    colunas.get(colunaTexto).trim();

            if (!nome.isEmpty() &&
                    !link.isEmpty()) {
                nomes.add(nome);
                links.add(link);
                textosPlanilha.add(texto);
                repeticoes.add(1);
            }
        }
    }

    private static int localizarColuna(
            ArrayList<String> cabecalhos,
            String nome) {
        for (int i = 0; i < cabecalhos.size(); i++) {
            String cabecalho = cabecalhos.get(i)
                    .replace("\uFEFF", "")
                    .trim();
            if (nome.equalsIgnoreCase(cabecalho)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
                "Cabeçalho obrigatório não encontrado: " + nome
        );
    }
}