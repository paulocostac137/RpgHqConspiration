package com.example.rpghqconspiration;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class PlanilhaParserTest {

    @Test
    public void usaTextoDaColunaCEmVezDaColunaE() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "nome,link,texto C,outra coluna,texto E\n" +
                        "gif,https://example.com/gif.gif,Texto da coluna C," +
                        "outro valor,Texto antigo da coluna E\n",
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
}
