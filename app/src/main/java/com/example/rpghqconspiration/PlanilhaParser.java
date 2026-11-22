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
        localizarColuna(cabecalhos, "Go1");
        localizarColuna(cabecalhos, "On1");
        localizarColuna(cabecalhos, "Out1");
        int quantidadeGruposMarcadores =
                localizarQuantidadeGruposMarcadores(cabecalhos);

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
                        extrairFaixasRepetidas(
                                colunas,
                                cabecalhos,
                                quantidadeGruposMarcadores,
                                numeroLinha + 1
                        )
                );
            }
        }
    }

    private static FaixaRepeticaoGif extrairFaixasRepetidas(
            ArrayList<String> colunas,
            ArrayList<String> cabecalhos,
            int quantidadeGrupos,
            int numeroLinha) {
        ArrayList<FaixaRepeticaoGif> faixas = new ArrayList<>();
            boolean grupoVazioAnterior = false;
            try {
            for (int grupo = 1; grupo <= quantidadeGrupos; grupo++) {
                int colunaGo = localizarColunaOpcional(
                        cabecalhos,
                        "Go" + grupo
                );
                int colunaOn = localizarColunaOpcional(
                        cabecalhos,
                        "On" + grupo
                );
                int colunaOut = localizarColunaOpcional(
                        cabecalhos,
                        "Out" + grupo
                );
                String go = valorColuna(colunas, colunaGo);
                String on = valorColuna(colunas, colunaOn);
                String out = valorColuna(colunas, colunaOut);
                boolean grupoVazio = go.isEmpty() && on.isEmpty() &&
                        out.isEmpty();
                if (grupoVazio) {
                    grupoVazioAnterior = true;
                    continue;
                }
                if (grupoVazioAnterior) {
                    throw new IllegalArgumentException(
                            "Os grupos de marcadores precisam ser preenchidos " +
                                    "em sequência na linha " + numeroLinha + "."
                    );
                }
                if (go.isEmpty() || on.isEmpty() || out.isEmpty()) {
                    throw new IllegalArgumentException(
                            "O grupo Go" + grupo + "–On" + grupo +
                                    "–Out" + grupo + " está incompleto na linha " +
                                    numeroLinha + "."
                    );
                }
                int primeiroQuadro = lerPrimeiroNumeroQuadro(
                        go,
                        "Go" + grupo,
                        numeroLinha
                );
                int quadroOn = lerPrimeiroNumeroQuadro(
                        on,
                        "On" + grupo,
                        numeroLinha
                );
                int ultimoQuadro = lerPrimeiroNumeroQuadro(
                        out,
                        "Out" + grupo,
                        numeroLinha
                );
                FaixaRepeticaoGif faixa = new FaixaRepeticaoGif(
                        primeiroQuadro,
                        quadroOn,
                        ultimoQuadro
                );
                if (!faixas.isEmpty() &&
                        primeiroQuadro <
                                faixas.get(faixas.size() - 1)
                                        .getUltimoQuadro()) {
                    throw new IllegalArgumentException(
                            "As faixas marcadas estão fora de ordem na linha " +
                                    numeroLinha + "."
                    );
                }
                faixas.add(faixa);
            }
            return FaixaRepeticaoGif.agrupar(faixas);
        } catch (IllegalArgumentException erro) {
            if (erro.getMessage() != null &&
                    erro.getMessage().contains("linha " + numeroLinha)) {
                throw erro;
            }
            throw new IllegalArgumentException(
                    "Faixa marcada inválida na linha " +
                            numeroLinha + ".",
                    erro
            );
        }
    }

    private static String valorColuna(
            ArrayList<String> colunas,
            int indiceColuna) {
        return indiceColuna < 0 || indiceColuna >= colunas.size()
                ? ""
                : colunas.get(indiceColuna).trim();
    }

    private static int localizarQuantidadeGruposMarcadores(
            ArrayList<String> cabecalhos) {
        int quantidade = 0;
        for (int grupo = 1; ; grupo++) {
            int go = localizarColunaOpcional(cabecalhos, "Go" + grupo);
            int on = localizarColunaOpcional(cabecalhos, "On" + grupo);
            int out = localizarColunaOpcional(cabecalhos, "Out" + grupo);
            if (go < 0 && on < 0 && out < 0) {
                break;
            }
            if (go < 0 || on < 0 || out < 0) {
                throw new IllegalArgumentException(
                        "Cada grupo de marcadores precisa ter Go" + grupo +
                                ", On" + grupo + " e Out" + grupo + "."
                );
            }
            quantidade = grupo;
        }
        return quantidade;
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