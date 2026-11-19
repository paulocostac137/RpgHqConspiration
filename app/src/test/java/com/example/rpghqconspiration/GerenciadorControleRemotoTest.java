package com.example.rpghqconspiration;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GerenciadorControleRemotoTest {

    private static final class ListenerDummy
            implements GerenciadorControleRemoto.Listener {

        Float progresso;
        boolean liberado;

        @Override
        public void onControleRecebido(float valor) {
            progresso = valor;
        }

        @Override
        public void onControleLiberado() {
            liberado = true;
        }
    }

    @Test
    public void limitaProgressoRecebidoEntreZeroEUm() throws Exception {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorControleRemoto controle =
                new GerenciadorControleRemoto(listener);

        controle.processar(new JSONObject()
                .put("active", true)
                .put("progress", 1.8));

        assertEquals(1f, listener.progresso, 0f);

        controle.processar(new JSONObject()
                .put("active", true)
                .put("progress", -0.5));

        assertEquals(0f, listener.progresso, 0f);
    }

    @Test
    public void liberaControleQuandoActiveEFalse() throws Exception {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorControleRemoto controle =
                new GerenciadorControleRemoto(listener);

        controle.processar(new JSONObject()
                .put("active", false)
                .put("progress", 0.5));

        assertTrue(listener.liberado);
    }

    @Test
    public void ignoraMensagemNula() {
        ListenerDummy listener = new ListenerDummy();
        GerenciadorControleRemoto controle =
                new GerenciadorControleRemoto(listener);

        controle.processar(null);

        assertTrue(listener.progresso == null);
        assertTrue(!listener.liberado);
    }
}
