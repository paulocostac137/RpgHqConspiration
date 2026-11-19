package com.example.rpghqconspiration;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PlanilhaParserTest {

    @Test
    public void usaTextoDaColunaCEmVezDaColunaE() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "nome,link,texto C,repeticoes,texto E\n" +
                        "gif,https://example.com/gif.gif,Texto da coluna C," +
                        "3,Texto antigo da coluna E\n",
                nomes,
                links,
                textos
        );

        assertEquals(1, textos.size());
        assertEquals("Texto da coluna C", textos.get(0));
    }

    @Test
    public void textoVazioNaColunaCNaoUsaColunaEComoAlternativa() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "nome,link,texto C,outra coluna,texto E\n" +
                        "gif,https://example.com/gif.gif,,," +
                        "Texto da coluna E\n",
                nomes,
                links,
                textos
        );

        assertEquals(1, textos.size());
        assertEquals("", textos.get(0));
    }

    @Test
    public void leRepeticoesDaColunaDNaMesmaLinhaDoGif() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();
        ArrayList<Integer> repeticoes = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "nome,link,texto,repeticoes\n" +
                        "gif1,https://example.com/1.gif,texto,3\n" +
                        "gif2,https://example.com/2.gif,texto,\n",
                nomes,
                links,
                textos,
                repeticoes
        );

        assertEquals(2, repeticoes.size());
        assertEquals(Integer.valueOf(3), repeticoes.get(0));
        assertEquals(Integer.valueOf(1), repeticoes.get(1));
    }

    @Test
    public void rejeitaRepeticaoNaoPositivaNaColunaD() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlanilhaParser.preencherListas(
                        "nome,link,texto,repeticoes\n" +
                                "gif,https://example.com/1.gif,texto,0\n",
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                )
        );
    }
}
