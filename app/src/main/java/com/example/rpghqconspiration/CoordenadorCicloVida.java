package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ExecutorService;

/**
 * Coordena o encerramento dos recursos utilizados
 * durante a execução da aplicação.
 *
 * Responsabilidades:
 * - Encerrar os coordenadores.
 * - Encerrar a reprodução.
 * - Encerrar o gerenciador de texto.
 * - Cancelar callbacks pendentes.
 * - Remover callbacks da comunicação.
 * - Liberar o renderizador.
 * - Encerrar o executor compartilhado.
 *
 * Não manipula Views.
 * Não controla a navegação.
 * Não limpa diretamente os estados dos gerenciadores
 * pertencentes a outros coordenadores.
 * Não encerra diretamente a conexão WebSocket,
 * pois ela pertence à ConexaoActivity.
 */
public final class CoordenadorCicloVida {

    private static final String TAG =
            "CoordenadorCicloVida";

    // =====================================================
    // EXECUÇÃO
    // =====================================================

    private final Handler handler;

    private final ExecutorService executor;

    // =====================================================
    // COORDENADORES
    // =====================================================

    private final CoordenadorInteracao coordenadorInteracao;

    private final CoordenadorFluxoGif coordenadorFluxoGif;

    private final CoordenadorPlanilhaGif coordenadorPlanilhaGif;

    private final CoordenadorExibicaoGif coordenadorExibicaoGif;

    // =====================================================
    // GERENCIADORES DE RECURSOS COMPARTILHADOS
    // =====================================================

    private final GerenciadorReproducaoGif gerenciadorReproducao;

    private final GerenciadorTextoPlanilha gerenciadorTextoPlanilha;

    private final GerenciadorComunicacaoServidor
            gerenciadorComunicacaoServidor;

    // =====================================================
    // RENDERIZAÇÃO
    // =====================================================

    private final GifFrameRenderer renderizadorGif;

    // =====================================================
    // ESTADO
    // =====================================================

    private boolean encerrado = false;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public CoordenadorCicloVida(
            Handler handler,
            ExecutorService executor,
            CoordenadorInteracao coordenadorInteracao,
            CoordenadorFluxoGif coordenadorFluxoGif,
            CoordenadorPlanilhaGif coordenadorPlanilhaGif,
            CoordenadorExibicaoGif coordenadorExibicaoGif,
            GerenciadorReproducaoGif gerenciadorReproducao,
            GerenciadorTextoPlanilha gerenciadorTextoPlanilha,
            GerenciadorComunicacaoServidor gerenciadorComunicacaoServidor,
            GifFrameRenderer renderizadorGif
    ) {

        this.handler = handler;

        this.executor = executor;

        this.coordenadorInteracao =
                coordenadorInteracao;

        this.coordenadorFluxoGif =
                coordenadorFluxoGif;

        this.coordenadorPlanilhaGif =
                coordenadorPlanilhaGif;

        this.coordenadorExibicaoGif =
                coordenadorExibicaoGif;

        this.gerenciadorReproducao =
                gerenciadorReproducao;

        this.gerenciadorTextoPlanilha =
                gerenciadorTextoPlanilha;

        this.gerenciadorComunicacaoServidor =
                gerenciadorComunicacaoServidor;

        this.renderizadorGif =
                renderizadorGif;
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    public void encerrar() {

        if (encerrado) {
            return;
        }

        encerrado = true;

        Log.d(
                TAG,
                "Iniciando encerramento dos recursos."
        );

        // -------------------------------------------------
        // CANCELAR CALLBACKS PENDENTES
        // -------------------------------------------------

        if (handler != null) {

            handler.removeCallbacksAndMessages(null);
        }

        // -------------------------------------------------
        // ENCERRAR COORDENADOR DE INTERAÇÃO
        // -------------------------------------------------

        if (coordenadorInteracao != null) {

            coordenadorInteracao.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR COORDENADOR DE EXIBIÇÃO
        // -------------------------------------------------

        if (coordenadorExibicaoGif != null) {

            coordenadorExibicaoGif.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR COORDENADOR DE FLUXO
        // -------------------------------------------------

        if (coordenadorFluxoGif != null) {

            coordenadorFluxoGif.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR COORDENADOR DA PLANILHA
        // -------------------------------------------------

        if (coordenadorPlanilhaGif != null) {

            coordenadorPlanilhaGif.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR REPRODUÇÃO
        // -------------------------------------------------

        if (gerenciadorReproducao != null) {

            gerenciadorReproducao.encerrar();
        }

        // -------------------------------------------------
        // ENCERRAR GERENCIADOR DE TEXTO
        // -------------------------------------------------

        if (gerenciadorTextoPlanilha != null) {

            gerenciadorTextoPlanilha.encerrar();
        }

        // -------------------------------------------------
        // REMOVER CALLBACKS DA COMUNICAÇÃO
        // -------------------------------------------------

        if (gerenciadorComunicacaoServidor != null) {

            gerenciadorComunicacaoServidor.removerCallback();
        }

        // -------------------------------------------------
        // LIBERAR RENDERIZADOR
        // -------------------------------------------------

        if (renderizadorGif != null) {

            renderizadorGif.liberar();
        }

        // -------------------------------------------------
        // ENCERRAR EXECUTOR COMPARTILHADO
        // -------------------------------------------------

        if (executor != null) {

            executor.shutdownNow();
        }

        Log.d(
                TAG,
                "Encerramento dos recursos concluído."
        );
    }

    // =====================================================
    // CONSULTA
    // =====================================================

    public boolean estaEncerrado() {

        return encerrado;
    }
}