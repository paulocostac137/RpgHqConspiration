package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GerenciadorNavegacaoGifTest {

    @Test
    public void navegaEmOrdemCircular() {
        GerenciadorNavegacaoGif navegacao =
                new GerenciadorNavegacaoGif();

        assertEquals(0, navegacao.proximo(3));
        assertEquals(1, navegacao.proximo(3));
        assertEquals(2, navegacao.proximo(3));
        assertEquals(0, navegacao.proximo(3));
        assertTrue(navegacao.possuiSelecao());
    }

    @Test
    public void retornaMenosUmSemItens() {
        GerenciadorNavegacaoGif navegacao =
                new GerenciadorNavegacaoGif();

        assertEquals(-1, navegacao.proximo(0));
        assertFalse(navegacao.possuiSelecao());
    }

    @Test
    public void reiniciarRemoveSelecao() {
        GerenciadorNavegacaoGif navegacao =
                new GerenciadorNavegacaoGif();
        navegacao.proximo(2);

        navegacao.reiniciar();

        assertFalse(navegacao.possuiSelecao());
        assertEquals(-1, navegacao.getIndiceAtual());
    }

    @Test
    public void selecionaIndiceSincronizadoSemAvancarSequencia() {
        GerenciadorNavegacaoGif navegacao =
                new GerenciadorNavegacaoGif();

        assertTrue(navegacao.selecionar(2, 3));
        assertEquals(2, navegacao.getIndiceAtual());
        assertEquals(0, navegacao.proximo(3));
    }

    @Test
    public void rejeitaIndiceSincronizadoForaDaLista() {
        GerenciadorNavegacaoGif navegacao =
                new GerenciadorNavegacaoGif();

        assertFalse(navegacao.selecionar(3, 3));
        assertEquals(-1, navegacao.getIndiceAtual());
    }
}
