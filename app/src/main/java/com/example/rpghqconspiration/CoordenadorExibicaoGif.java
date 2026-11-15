package com.example.rpghqconspiration;

import android.graphics.Movie;
import android.util.Log;
import android.widget.FrameLayout;

import java.io.File;

/**
 * Coordena o processo de exibição de um GIF.
 *
 * Responsabilidades:
 * - Interromper a reprodução anterior.
 * - Solicitar a preparação dos controles.
 * - Solicitar a decodificação do GIF.
 * - Atualizar o Movie atual.
 * - Iniciar a animação e o texto correspondente.
 *
 * Não realiza downloads nem decodifica diretamente os arquivos.
 * Não acessa diretamente os gerenciadores de interação.
 */
public final class CoordenadorExibicaoGif {

    private static final String TAG = "CoordenadorExibicaoGif";

    private final FrameLayout areaGif;

    private final GerenciadorDecodificacaoGif decodificador;

    private final GerenciadorReproducaoGif reproducao;

    private final GerenciadorTextoPlanilha textoPlanilha;

    private final CoordenadorInteracao coordenadorInteracao;

    private final Listener listener;

    private long geracaoExibicao = 0;

    private boolean encerrado = false;

    public interface Listener {

        boolean atividadeDisponivel();

        void definirMovie(Movie movie);

        void definirControleRemotoAtivo(boolean ativo);

        void desenharQuadro();

        void iniciarAnimacao();

        void atualizarVisibilidadeControles();

        void atualizarStatus(String mensagem);
    }

    public CoordenadorExibicaoGif(
            FrameLayout areaGif,
            GerenciadorDecodificacaoGif decodificador,
            GerenciadorReproducaoGif reproducao,
            GerenciadorTextoPlanilha textoPlanilha,
            CoordenadorInteracao coordenadorInteracao,
            Listener listener
    ) {

        if (areaGif == null ||
                decodificador == null ||
                reproducao == null ||
                textoPlanilha == null ||
                coordenadorInteracao == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "Os componentes obrigatórios não podem ser nulos."
            );
        }

        this.areaGif = areaGif;
        this.decodificador = decodificador;
        this.reproducao = reproducao;
        this.textoPlanilha = textoPlanilha;
        this.coordenadorInteracao = coordenadorInteracao;
        this.listener = listener;
    }

    private boolean disponivel() {

        return !encerrado &&
                areaGif != null &&
                listener != null &&
                listener.atividadeDisponivel();
    }

    /**
     * Inicia o processo de exibição de um GIF.
     */
    public void exibir(
            File arquivo,
            String nome,
            String texto,
            int indiceExibido,
            int quantidadeTotal
    ) {

        if (!disponivel()) {
            return;
        }

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() == 0) {

            listener.atualizarStatus(
                    "Arquivo GIF inválido."
            );

            return;
        }

        final long geracaoAtual = ++geracaoExibicao;

        // 1. Interromper a reprodução anterior.

        reproducao.reiniciar();

        textoPlanilha.parar();

        // 2. Preparar os controles para o novo GIF.

        coordenadorInteracao.prepararParaNovoGif();

        listener.definirControleRemotoAtivo(false);

        // 3. Limpar o GIF anterior.

        listener.definirMovie(null);

        listener.atualizarVisibilidadeControles();

        // 4. Solicitar a decodificação.

        decodificador.decodificar(
                arquivo,
                new GerenciadorDecodificacaoGif.Listener() {

                    @Override
                    public void onDecodificacaoConcluida(
                            Movie resultado
                    ) {

                        if (!disponivel() ||
                                geracaoAtual != geracaoExibicao) {

                            return;
                        }

                        if (resultado == null) {

                            tratarErro(
                                    arquivo,
                                    "Não foi possível decodificar o GIF."
                            );

                            return;
                        }

                        // 5. Atualizar o GIF atual.

                        listener.definirMovie(resultado);

                        reproducao.reiniciar();

                        // 6. Atualizar a interface na thread principal.

                        areaGif.post(() -> {

                            if (!disponivel() ||
                                    geracaoAtual != geracaoExibicao) {

                                return;
                            }

                            listener.desenharQuadro();

                            listener.iniciarAnimacao();

                            textoPlanilha.iniciar(texto);

                            listener.atualizarStatus(
                                    nome + " | " +
                                            indiceExibido + "/" +
                                            quantidadeTotal
                            );
                        });
                    }

                    @Override
                    public void onErro(Exception erro) {

                        if (!disponivel() ||
                                geracaoAtual != geracaoExibicao) {

                            return;
                        }

                        tratarErro(
                                arquivo,
                                "Erro ao abrir GIF: " +
                                        erro.getMessage()
                        );

                        Log.e(
                                TAG,
                                "Erro ao decodificar GIF.",
                                erro
                        );
                    }
                }
        );
    }

    /**
     * Trata falhas durante a exibição.
     */
    private void tratarErro(
            File arquivo,
            String mensagem
    ) {

        reproducao.reiniciar();

        textoPlanilha.parar();

        listener.definirMovie(null);

        if (arquivo != null && arquivo.exists()) {

            arquivo.delete();
        }

        listener.atualizarStatus(mensagem);
    }

    /**
     * Invalida callbacks de uma exibição anterior.
     */
    public void invalidar() {

        geracaoExibicao++;
    }

    /**
     * Encerra o coordenador.
     */
    public void encerrar() {

        if (encerrado) {
            return;
        }

        encerrado = true;

        invalidar();
    }

    public boolean estaEncerrado() {

        return encerrado;
    }
}