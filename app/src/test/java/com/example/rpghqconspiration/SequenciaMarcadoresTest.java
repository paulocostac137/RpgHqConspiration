package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SequenciaMarcadoresTest {

    @Test
    public void percorreTresCiclosEmOrdemCrescenteComQuadrosLivres() {
        SequenciaMarcadores sequencia = new SequenciaMarcadores();
        char[] marcacoes = {'G', 'O', 'T', 'G', 'O', 'T', 'G', 'O', 'T'};

        for (int i = 0; i < marcacoes.length; i++) {
            assertTrue(sequencia.podeAdicionar(marcacoes[i], i * 2));
            assertTrue(sequencia.adicionar(marcacoes[i], i * 2));
        }

        assertEquals('\0', sequencia.obterProximaMarcacao());
        assertFalse(sequencia.adicionar('G', 20));
        assertFalse(sequencia.adicionar('O', 20));
        assertFalse(sequencia.adicionar('T', 20));
    }

    @Test
    public void impedePularEtapasERejeitaMarcacoesForaDaOrdem() {
        SequenciaMarcadores sequencia = new SequenciaMarcadores();

        assertFalse(sequencia.adicionar('O', 0));
        assertFalse(sequencia.adicionar('T', 0));
        assertTrue(sequencia.adicionar('G', 3));
        assertFalse(sequencia.adicionar('G', 4));
        assertFalse(sequencia.adicionar('O', 2));
        assertTrue(sequencia.adicionar('O', 6));
        assertTrue(sequencia.adicionar('T', 7));
        assertEquals('G', sequencia.obterProximaMarcacao());
    }

    @Test
    public void atualizaRotuloOnAoCompletarCadaCiclo() {
        SequenciaMarcadores sequencia = new SequenciaMarcadores();

        assertEquals("On", sequencia.obterRotuloOn());
        sequencia.adicionar('G', 1);
        sequencia.adicionar('O', 3);
        assertEquals("On", sequencia.obterRotuloOn());
        sequencia.adicionar('T', 4);
        assertEquals("On2", sequencia.obterRotuloOn());

        sequencia.adicionar('G', 5);
        sequencia.adicionar('O', 7);
        sequencia.adicionar('T', 8);
        assertEquals("On3", sequencia.obterRotuloOn());

        sequencia.adicionar('G', 9);
        sequencia.adicionar('O', 11);
        sequencia.adicionar('T', 12);
        assertEquals("On3", sequencia.obterRotuloOn());
    }

    @Test
    public void exigeOrdemCrescenteSemIntercalarCiclos() {
        SequenciaMarcadores sequencia = new SequenciaMarcadores();

        assertTrue(sequencia.adicionar('G', 2));
        assertFalse(sequencia.podeAdicionar('O', 1));
        assertFalse(sequencia.podeAdicionar('T', 3));
        assertTrue(sequencia.adicionar('O', 5));
        assertFalse(sequencia.adicionar('T', 4));
        assertTrue(sequencia.adicionar('T', 8));

        assertTrue(sequencia.adicionar('G', 10));
        assertFalse(sequencia.adicionar('O', 9));
        assertTrue(sequencia.adicionar('O', 12));
        assertTrue(sequencia.adicionar('T', 15));

        assertTrue(sequencia.adicionar('G', 16));
        assertTrue(sequencia.adicionar('O', 20));
        assertTrue(sequencia.adicionar('T', 22));
    }

    @Test
    public void exigeConclusaoDoPrimeiroCicloAntesDePermitirFinalizar() {
        SequenciaMarcadores sequencia = new SequenciaMarcadores();

        assertFalse(sequencia.temPeloMenosTresMarcacoes());
        sequencia.adicionar('G', 0);
        assertFalse(sequencia.temPeloMenosTresMarcacoes());
        sequencia.adicionar('O', 1);
        assertFalse(sequencia.temPeloMenosTresMarcacoes());
        sequencia.adicionar('T', 2);
        assertTrue(sequencia.temPeloMenosTresMarcacoes());
    }
}
