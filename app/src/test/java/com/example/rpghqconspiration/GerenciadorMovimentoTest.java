package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GerenciadorMovimentoTest {

    private static final class ListenerDummy
            implements GerenciadorMovimento.Listener {

        int avancos;
        int retomadas;

        @Override
        public boolean possuiGif() {
            return true;
        }

        @Override
        public void avancarParaProximoGif() {
            avancos++;
        }

        @Override
        public void retomarAnimacao() {
            retomadas++;
        }
    }

    @Test
    public void naoContaMovimentoDentroDaAreaCentral() {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorMovimento movimento =
                new GerenciadorMovimento(listener);

        movimento.registrarProgresso(0.5f, true, false);

        assertEquals(0, movimento.getCiclosConcluidos());
    }

    @Test
    public void tresCiclosCompletosSolicitamAvanco() {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorMovimento movimento =
                new GerenciadorMovimento(listener);

        for (int i = 0; i < 3; i++) {
            movimento.registrarProgresso(0.10f, true, false);
            movimento.registrarProgresso(0.90f, true, false);
            movimento.registrarProgresso(0.10f, true, false);
        }

        assertEquals(3, movimento.getCiclosConcluidos());

        movimento.finalizarMovimentoSeLiberado(false, false);

        assertEquals(1, listener.avancos);
        assertEquals(0, movimento.getCiclosConcluidos());
    }

    @Test
    public void progressoInvalidoENaoAtivoNaoAlteraContagem() {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorMovimento movimento =
                new GerenciadorMovimento(listener);

        movimento.registrarProgresso(Float.NaN, true, false);
        movimento.registrarProgresso(Float.POSITIVE_INFINITY, false, true);
        movimento.registrarProgresso(0.10f, false, false);

        assertEquals(0, movimento.getCiclosConcluidos());
    }
}
