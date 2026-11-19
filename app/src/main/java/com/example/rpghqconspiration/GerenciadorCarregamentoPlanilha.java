package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Responsável pelo carregamento assíncrono
 * e interpretação dos dados da planilha.
 *
 * Não manipula Views nem controla a navegação
 * ou a reprodução dos GIFs.
 */
public final class GerenciadorCarregamentoPlanilha {

    private static final String TAG =
            "GerenciadorCarregamentoPlanilha";

    // =====================================================
    // RESULTADO DO CARREGAMENTO
    // =====================================================

    public static final class Resultado {

        private final ArrayList<String> nomes;
        private final ArrayList<String> links;
        private final ArrayList<String> textos;
        private final ArrayList<Integer> repeticoes;

        private Resultado(
                ArrayList<String> nomes,
                ArrayList<String> links,
                ArrayList<String> textos,
                ArrayList<Integer> repeticoes) {

            this.nomes = new ArrayList<>(nomes);
            this.links = new ArrayList<>(links);
            this.textos = new ArrayList<>(textos);
            this.repeticoes = new ArrayList<>(repeticoes);
        }

        public ArrayList<String> getNomes() {
            return new ArrayList<>(nomes);
        }

        public ArrayList<String> getLinks() {
            return new ArrayList<>(links);
        }

        public ArrayList<String> getTextos() {
            return new ArrayList<>(textos);
        }

        public ArrayList<Integer> getRepeticoes() {
            return new ArrayList<>(repeticoes);
        }

        public int getQuantidade() {
            return nomes.size();
        }

        public boolean estaVazio() {
            return nomes.isEmpty();
        }
    }

    // =====================================================
    // COMUNICAÇÃO COM A MAINACTIVITY
    // =====================================================

    public interface Listener {

        void onCarregamentoConcluido(Resultado resultado);

        void onErro(Exception erro);
    }

    // =====================================================
    // DEPENDÊNCIAS
    // =====================================================

    private final PlanilhaRepository planilhaRepository;

    private final ExecutorService executor;

    private final Handler handler;

    private final Listener listener;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public GerenciadorCarregamentoPlanilha(
            PlanilhaRepository planilhaRepository,
            ExecutorService executor,
            Handler handler,
            Listener listener) {

        if (planilhaRepository == null ||
                executor == null ||
                handler == null ||
                listener == null) {

            throw new IllegalArgumentException(
                    "As dependências não podem ser nulas."
            );
        }

        this.planilhaRepository = planilhaRepository;
        this.executor = executor;
        this.handler = handler;
        this.listener = listener;
    }

    // =====================================================
    // CARREGAR PLANILHA
    // =====================================================

    public void carregar() {

        executor.execute(() -> {

            try {

                Log.d(
                        TAG,
                        "Iniciando consulta à planilha."
                );

                // -----------------------------------------
                // BAIXAR CONTEÚDO CSV
                // -----------------------------------------

                String conteudo =
                        planilhaRepository.baixarConteudo();

                // -----------------------------------------
                // INTERPRETAR DADOS
                // -----------------------------------------

                ArrayList<String> nomes =
                        new ArrayList<>();

                ArrayList<String> links =
                        new ArrayList<>();

                ArrayList<String> textos =
                        new ArrayList<>();
                ArrayList<Integer> repeticoes =
                        new ArrayList<>();

                PlanilhaParser.preencherListas(
                        conteudo,
                        nomes,
                        links,
                        textos,
                        repeticoes
                );

                Resultado resultado = new Resultado(
                        nomes,
                        links,
                        textos,
                        repeticoes
                );

                Log.d(
                        TAG,
                        "Planilha carregada. Registros: "
                                + resultado.getQuantidade()
                );

                // -----------------------------------------
                // DEVOLVER RESULTADO À THREAD PRINCIPAL
                // -----------------------------------------

                handler.post(() ->
                        listener.onCarregamentoConcluido(
                                resultado
                        )
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Erro ao carregar planilha.",
                        e
                );

                handler.post(() ->
                        listener.onErro(e)
                );
            }
        });
    }
}