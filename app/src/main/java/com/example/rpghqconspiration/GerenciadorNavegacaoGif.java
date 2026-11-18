package com.example.rpghqconspiration;

/**
 * Responsável exclusivamente pela navegação
 * sequencial entre os GIFs disponíveis.
 *
 * Não realiza downloads, não manipula Views
 * e não controla a reprodução das animações.
 */
public final class GerenciadorNavegacaoGif {

    private int indiceAtual = -1;

    /**
     * Avança para o próximo GIF.
     *
     * Retorna -1 quando não existem GIFs.
     * Ao chegar ao final, retorna ao primeiro.
     */
    public int proximo(int quantidade) {

        if (quantidade <= 0) {
            indiceAtual = -1;
            return -1;
        }

        indiceAtual++;

        if (indiceAtual >= quantidade) {
            indiceAtual = 0;
        }

        return indiceAtual;
    }

    public boolean selecionar(int indice, int quantidade) {
        if (quantidade <= 0 || indice < 0 || indice >= quantidade) {
            return false;
        }

        indiceAtual = indice;
        return true;
    }

    /**
     * Reinicia a navegação.
     */
    public void reiniciar() {
        indiceAtual = -1;
    }

    /**
     * Retorna o índice atualmente selecionado.
     */
    public int getIndiceAtual() {
        return indiceAtual;
    }

    /**
     * Informa se existe um GIF selecionado.
     */
    public boolean possuiSelecao() {
        return indiceAtual >= 0;
    }
}