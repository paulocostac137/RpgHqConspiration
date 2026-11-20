package com.example.rpghqconspiration;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PlanilhaParserTest {

    @Test
    public void usaExpressComoColunaDeTexto() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "Pov000,Link,Express,Go1,On1,Out1\n" +
                        "Pov001,https://example.com/gif.gif,Texto do GIF,1,2,3\n",
                nomes,
                links,
                textos
        );

        assertEquals(1, textos.size());
        assertEquals("Texto do GIF", textos.get(0));
    }

    @Test
    public void leNovaEstruturaSemInterpretarMarcadoresComoRepeticoes() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();
        ArrayList<Integer> repeticoes = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "Pov000,Link,Express,Go1,On1,Out1\n" +
                        "Pov001,https://example.com/gif.gif," +
                        "Texto do GIF,\"1,2\",3,\"4,5\"\n",
                nomes,
                links,
                textos,
                repeticoes
        );

        assertEquals(1, nomes.size());
        assertEquals("Pov001", nomes.get(0));
        assertEquals("https://example.com/gif.gif", links.get(0));
        assertEquals("Texto do GIF", textos.get(0));
        assertEquals(Integer.valueOf(1), repeticoes.get(0));
    }

    @Test
    public void textoExpressVazioPermaneceVazio() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "Pov000,Link,Express,Go1,On1,Out1\n" +
                        "Pov001,https://example.com/gif.gif,,1,2,3\n",
                nomes,
                links,
                textos
        );

        assertEquals(1, textos.size());
        assertEquals("", textos.get(0));
    }

    @Test
    public void colunasDeMarcadoresNaoAlteramRepeticoesDoApp() {
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();
        ArrayList<Integer> repeticoes = new ArrayList<>();

        PlanilhaParser.preencherListas(
                "Pov000,Link,Express,Go1,On1,Out1\n" +
                        "Pov001,https://example.com/1.gif,texto,\"1,2\",3,\"4,5\"\n" +
                        "Pov002,https://example.com/2.gif,texto,,, \n",
                nomes,
                links,
                textos,
                repeticoes
        );

        assertEquals(2, repeticoes.size());
        assertEquals(Integer.valueOf(1), repeticoes.get(0));
        assertEquals(Integer.valueOf(1), repeticoes.get(1));
    }

    @Test
    public void rejeitaPlanilhaSemCabecalhoDoFormatoDefinitivo() {
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
