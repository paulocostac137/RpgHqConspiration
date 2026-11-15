package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Coordena o fluxo entre:
 *
 * 1. Seleção do próximo GIF.
 * 2. Validação da posição selecionada.
 * 3. Solicitação do carregamento.
 * 4. Comunicação do resultado para a camada responsável pela interface.
 *
 * Não manipula Views diretamente.
 * Não decodifica GIFs.
 * Não controla a reprodução.
 * Não é responsável por encerrar o ExecutorService compartilhado.
 */
public final class CoordenadorFluxoGif
        implements GerenciadorCarregamentoGif.Listener {

    private static final String TAG = "CoordenadorFluxoGif";

    private final List<String> nomes;
    private final List<String> links;

    private final GerenciadorNavegacaoGif navegacao;
    private final GerenciadorCarregamentoGif carregamento;

    private final Listener listener;

    private String nomeGifSolicitado = "";

    private boolean encerrado = false;

    /**
     * Contrato de comunicação com a Activity ou outro coordenador.
     */
    public interface Listener {

        boolean atividadeDisponivel();

        boolean estaCarregando();

        void definirCarregando(boolean carregando);

        void definirNomeGifSolicitado(String nome);

        void definirBotaoProximoHabilitado(boolean habilitado);

        void atualizarStatus(String mensagem);

        void exibirGif(File arquivo, String nome);
    }

    /**
     * Constrói o fluxo de navegação e carregamento.
     *
     * As listas recebidas são mantidas como referências.
     * Portanto, quando a planilha atualizar essas listas,
     * o coordenador utilizará os dados atualizados.
     */
    public CoordenadorFluxoGif(
            List<String> nomes,
            List<String> links,
            GifRepository gifRepository,
            ExecutorService executor,
            Handler handler,
            Listener listener
    ) {

        if (nomes == null ||
                links == null ||
                gifRepository == null ||
                executor == null ||
                handler == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "As dependências não podem ser nulas."
            );
        }

        this.nomes = nomes;
        this.links = links;
        this.listener = listener;

        navegacao = new GerenciadorNavegacaoGif();

        carregamento = new GerenciadorCarregamentoGif(
                gifRepository,
                executor,
                handler,
                this
        );
    }

    /**
     * Verifica se o coordenador ainda pode executar operações.
     */
    private boolean disponivel() {

        return !encerrado &&
                listener != null &&
                listener.atividadeDisponivel();
    }

    /**
     * Solicita o próximo GIF da lista.
     *
     * A navegação é sequencial e retorna ao primeiro
     * elemento quando chega ao final.
     */
    public void carregarProximo() {

        if (!disponivel() ||
                listener.estaCarregando()) {

            return;
        }

        if (nomes.isEmpty()) {

            listener.atualizarStatus(
                    "Nenhum GIF disponível."
            );

            return;
        }

        int indiceSelecionado =
                navegacao.proximo(nomes.size());

        if (indiceSelecionado < 0) {
            return;
        }

        carregarPosicao(indiceSelecionado);
    }

    /**
     * Solicita o carregamento de uma posição específica.
     */
    public void carregarPosicao(int posicao) {

        if (!disponivel() ||
                listener.estaCarregando()) {

            return;
        }

        if (posicao < 0 ||
                posicao >= nomes.size() ||
                posicao >= links.size()) {

            Log.e(
                    TAG,
                    "Posição inválida para carregamento: " +
                            posicao
            );

            return;
        }

        nomeGifSolicitado = nomes.get(posicao);

        String link = links.get(posicao);

        listener.definirNomeGifSolicitado(
                nomeGifSolicitado
        );

        listener.atualizarStatus(
                "Carregando: " + nomeGifSolicitado
        );

        carregamento.carregar(link);
    }

    /**
     * Reinicia o índice da navegação.
     *
     * Deve ser chamado quando uma nova lista de GIFs
     * for carregada da planilha.
     */
    public void reiniciarNavegacao() {

        if (encerrado) {
            return;
        }

        navegacao.reiniciar();

        nomeGifSolicitado = "";

        listener.definirNomeGifSolicitado("");
    }

    /**
     * Retorna o índice atualmente selecionado.
     */
    public int getIndiceAtual() {

        return navegacao.getIndiceAtual();
    }

    /**
     * Informa a quantidade atual de GIFs disponíveis.
     */
    public int getQuantidade() {

        return nomes.size();
    }

    /**
     * Informa se existe um GIF selecionado.
     */
    public boolean possuiSelecao() {

        return navegacao.possuiSelecao();
    }

    /**
     * Chamado quando o GerenciadorCarregamentoGif
     * inicia a obtenção do arquivo.
     */
    @Override
    public void onCarregamentoIniciado() {

        if (!disponivel()) {
            return;
        }

        listener.definirCarregando(true);

        listener.definirBotaoProximoHabilitado(false);

        listener.atualizarStatus(
                "Carregando: " + nomeGifSolicitado
        );
    }

    /**
     * Chamado quando o arquivo GIF foi obtido.
     */
    @Override
    public void onCarregamentoConcluido(File arquivo) {

        if (!disponivel()) {
            return;
        }

        listener.definirCarregando(false);

        listener.definirBotaoProximoHabilitado(true);

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() <= 0) {

            onErro(
                    new IllegalStateException(
                            "O arquivo GIF recebido é inválido."
                    )
            );

            return;
        }

        listener.exibirGif(
                arquivo,
                nomeGifSolicitado
        );
    }

    /**
     * Chamado quando ocorre uma falha no carregamento.
     */
    @Override
    public void onErro(Exception erro) {

        if (!disponivel()) {
            return;
        }

        listener.definirCarregando(false);

        listener.definirBotaoProximoHabilitado(true);

        String mensagem = erro != null
                ? erro.getMessage()
                : "Erro desconhecido.";

        listener.atualizarStatus(
                "Erro ao obter GIF: " + mensagem
        );

        Log.e(
                TAG,
                "Erro ao carregar GIF.",
                erro
        );
    }

    /**
     * Impede novas operações e ignora callbacks posteriores.
     *
     * O ExecutorService não é encerrado aqui porque
     * pertence à camada que o criou.
     */
    public void encerrar() {

        encerrado = true;

        nomeGifSolicitado = "";
    }

    public boolean estaEncerrado() {

        return encerrado;
    }
}