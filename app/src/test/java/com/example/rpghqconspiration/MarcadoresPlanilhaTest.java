package com.example.rpghqconspiration;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;

public class MarcadoresPlanilhaTest {

    @Test
    public void distribuiCadaCicloEmSuasColunasGoOnOut() {
        assertArrayEquals(
                new String[]{"2", "4", "6", "8", "10", "12", "14", "16", "18"},
                MarcadoresPlanilha.colunasPorCiclo(
                        Arrays.asList(1, 3, 5, 7, 9, 11, 13, 15, 17)
                )
        );
    }

    @Test
    public void deixaVaziasAsColunasDeMarcacoesAindaNaoFeitas() {
        assertArrayEquals(
                new String[]{"3", "5", "7", "", "", "", "", "", ""},
                MarcadoresPlanilha.colunasPorCiclo(
                        Arrays.asList(2, 4, 6)
                )
        );
    }

    @Test
    public void semMarcacoesGeraNoveColunasVazias() {
        assertArrayEquals(
                new String[]{"", "", "", "", "", "", "", "", ""},
                MarcadoresPlanilha.colunasPorCiclo(Collections.emptyList())
        );
    }
}
