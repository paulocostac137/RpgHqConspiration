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
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public static void preencherListas(
            String conteudo,
            ArrayList<String> nomes,
            ArrayList<String> links,
            ArrayList<String> textosPlanilha,
            ArrayList<Integer> repeticoes) {
        preencherListas(
                conteudo,
                nomes,
                links,
                textosPlanilha,
                repeticoes,
                new ArrayList<>()
        );
    }

    public static void preencherListas(
            String conteudo,
            ArrayList<String> nomes,
            ArrayList<String> links,
            ArrayList<String> textosPlanilha,
            ArrayList<Integer> repeticoes,
            ArrayList<FaixaRepeticaoGif> faixasRepetidas) {

        nomes.clear();
        links.clear();
        textosPlanilha.clear();
        repeticoes.clear();
        faixasRepetidas.clear();

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
        int colunaGo1 = localizarColuna(cabecalhos, "Go1");
        localizarColuna(cabecalhos, "On1");
        localizarColuna(cabecalhos, "Out1");
        int colunaOut1 = localizarColunaOpcional(cabecalhos, "Out1");

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
                faixasRepetidas.add(
                        extrairFaixaRepetida(
                                colunas,
                                colunaGo1,
                                colunaOut1,
                                numeroLinha + 1
                        )
                );
            }
        }
    }

    private static FaixaRepeticaoGif extrairFaixaRepetida(
            ArrayList<String> colunas,
            int colunaGo1,
            int colunaOut1,
            int numeroLinha) {
        if (colunaOut1 < 0 ||
                colunaGo1 >= colunas.size() ||
                colunaOut1 >= colunas.size()) {
            return null;
        }

        String go1 = colunas.get(colunaGo1).trim();
        String out1 = colunas.get(colunaOut1).trim();
        if (go1.isEmpty() || out1.isEmpty()) {
            return null;
        }

        int primeiroQuadro = lerPrimeiroNumeroQuadro(
                go1,
                "Go1",
                numeroLinha
        );
        int ultimoQuadro = lerPrimeiroNumeroQuadro(
                out1,
                "Out1",
                numeroLinha
        );
        try {
            return new FaixaRepeticaoGif(primeiroQuadro, ultimoQuadro);
        } catch (IllegalArgumentException erro) {
            throw new IllegalArgumentException(
                    "Faixa Go1–Out1 inválida na linha " + numeroLinha + ".",
                    erro
            );
        }
    }

    private static int lerPrimeiroNumeroQuadro(
            String valor,
            String nomeColuna,
            int numeroLinha) {
        try {
            String primeiro = valor.split(",", -1)[0].trim();
            int numero = Integer.parseInt(primeiro);
            if (numero > 0) {
                return numero;
            }
        } catch (NumberFormatException ignored) {
            // Report the row and column in the validation error below.
        }
        throw new IllegalArgumentException(
                "A coluna " + nomeColuna +
                        " precisa conter um único número de quadro positivo " +
                        "na linha " + numeroLinha + "."
        );
    }

    private static int localizarColuna(
            ArrayList<String> cabecalhos,
            String nome) {
        int coluna = localizarColunaOpcional(cabecalhos, nome);
        if (coluna >= 0) {
            return coluna;
        }
        throw new IllegalArgumentException(
                "Cabeçalho obrigatório não encontrado: " + nome
        );
    }

    private static int localizarColunaOpcional(
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
        return -1;
    }
}