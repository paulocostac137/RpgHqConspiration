package com.example.rpghqconspiration;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class CsvParserTest {

    @Test
    public void respeitaVirgulaDentroDeAspas() {
        ArrayList<String> colunas =
                CsvParser.separar("nome,\"texto, com virgula\",link");

        assertEquals("nome", colunas.get(0));
        assertEquals("texto, com virgula", colunas.get(1));
        assertEquals("link", colunas.get(2));
    }

    @Test
    public void interpretaAspasDuplasEscapadas() {
        ArrayList<String> colunas =
                CsvParser.separar("\"texto \"\"especial\"\"\",\"link\"");

        assertEquals("texto \"especial\"", colunas.get(0));
        assertEquals("link", colunas.get(1));
    }
}
