package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Coordena a integração entre o carregamento da planilha
 * e a disponibilização dos GIFs para navegação.
 *
 * Responsabilidades:
 * - Solicitar o carregamento da planilha.
 * - Receber os dados interpretados.
 * - Atualizar as listas compartilhadas de nomes, links e textos.
 * - Reiniciar a navegação após uma nova consulta.
 * - Iniciar o primeiro GIF quando houver dados.
 * - Comunicar o resultado à camada de interface.
 *
 * Não realiza requisições HTTP diretamente.
 * Não interpreta CSV diretamente.
 * Não manipula Views.
 * Não baixa nem decodifica GIFs.
 */
public final class CoordenadorPlanilhaGif
        implements GerenciadorCarregamentoPlanilha.Listener {

    private static final String TAG = "CoordenadorPlanilhaGif";

    private final List<String> nomes;
    private final List<String> links;
    private final List<String> textos;

    private final GerenciadorCarregamentoPlanilha carregamentoPlanilha;

    private final CoordenadorFluxoGif fluxoGif;

    private final Listener listener;

    private boolean encerrado = false;

    /**
     * Comunicação com a camada responsável pela interface.
     */
    public interface Listener {

        boolean atividadeDisponivel();

        void atualizarStatus(String mensagem);

    }

    public CoordenadorPlanilhaGif(
            PlanilhaRepository planilhaRepository,
            ExecutorService executor,
            Handler handler,
            List<String> nomes,
            List<String> links,
            List<String> textos,
            CoordenadorFluxoGif fluxoGif,
            Listener listener
    ) {

        if (planilhaRepository == null ||
                executor == null ||
                handler == null ||
                nomes == null ||
                links == null ||
                textos == null ||
                fluxoGif == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "As dependências não podem ser nulas."
            );
        }

        this.nomes = nomes;
        this.links = links;
        this.textos = textos;

        this.fluxoGif = fluxoGif;
        this.listener = listener;

        carregamentoPlanilha =
                new GerenciadorCarregamentoPlanilha(
                        planilhaRepository,
                        executor,
                        handler,
                        this
                );
    }

    /**
     * Verifica se o coordenador pode continuar executando.
     */
    private boolean disponivel() {

        return !encerrado &&
                listener != null &&
                listener.atividadeDisponivel();
    }

    /**
     * Solicita uma nova consulta à planilha.
     */
    public void carregar() {

        if (!disponivel()) {
            return;
        }

        listener.atualizarStatus(
                "Consultando planilha..."
        );

        carregamentoPlanilha.carregar();
    }

    /**
     * Recebe os dados após o carregamento e a interpretação
     * da planilha terem sido concluídos.
     */
    @Override
    public void onCarregamentoConcluido(
            GerenciadorCarregamentoPlanilha.Resultado resultado
    ) {

        if (!disponivel()) {
            return;
        }

        if (resultado == null) {

            onErro(
                    new IllegalStateException(
                            "O resultado da planilha está vazio."
                    )
            );

            return;
        }

        // Atualizar os dados compartilhados.

        nomes.clear();
        links.clear();
        textos.clear();

        nomes.addAll(resultado.getNomes());
        links.addAll(resultado.getLinks());
        textos.addAll(resultado.getTextos());

        // Reiniciar a navegação para a nova lista.

        fluxoGif.reiniciarNavegacao();

        // Verificar se existem GIFs disponíveis.

        if (resultado.estaVazio()) {

            listener.atualizarStatus(
                    "Nenhum GIF encontrado."
            );

            return;
        }

        listener.atualizarStatus(
                resultado.getQuantidade() +
                        " GIFs disponíveis."
        );

        // Iniciar a sequência pelo primeiro GIF.

        fluxoGif.carregarProximo();

        Log.d(
                TAG,
                "Dados da planilha atualizados. Registros: " +
                        resultado.getQuantidade()
        );
    }

    /**
     * Recebe erros ocorridos durante a consulta
     * ou interpretação da planilha.
     */
    @Override
    public void onErro(Exception erro) {

        if (!disponivel()) {
            return;
        }

        String mensagem = erro != null
                ? erro.getMessage()
                : "Erro desconhecido.";

        listener.atualizarStatus(
                "Erro ao consultar planilha: " + mensagem
        );

        Log.e(
                TAG,
                "Falha ao carregar os dados da planilha.",
                erro
        );
    }

    /**
     * Encerra o coordenador e impede o processamento
     * de novos resultados.
     *
     * O ExecutorService é compartilhado e não é encerrado aqui.
     */
    public void encerrar() {

        encerrado = true;
    }

    public boolean estaEncerrado() {

        return encerrado;
    }

    public int getQuantidadeRegistros() {

        return nomes.size();
    }

    public boolean estaVazio() {

        return nomes.isEmpty();
    }
}