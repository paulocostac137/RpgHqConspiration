package com.example.rpghqconspiration;

import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONObject;

/**
 * Coordena todas as interações manuais e remotas.
 *
 * Responsabilidades:
 * - Coordenar o controle multitouch.
 * - Coordenar o controle remoto.
 * - Processar o progresso dos movimentos.
 * - Coordenar a reprodução durante as interações.
 * - Coordenar a contagem dos movimentos.
 * - Decidir quando retomar ou avançar o GIF.
 * - Preparar os controles para um novo GIF.
 *
 * Não realiza diretamente:
 * - Decodificação de GIF.
 * - Carregamento de arquivos.
 * - Renderização de quadros.
 * - Comunicação direta com o servidor.
 * - Manipulação de outras Activities.
 */
public final class CoordenadorInteracao implements
        GerenciadorControleMultitoque.Listener,
        GerenciadorControleRemoto.Listener,
        GerenciadorMovimento.Listener {

    private static final String TAG = "CoordenadorInteracao";

    // =====================================================
    // COMUNICAÇÃO COM O APLICATIVO
    // =====================================================

    public interface Host {

        boolean atividadeDisponivel();

        boolean possuiGif();

        int obterDuracaoGif();

        void pausarAnimacao();

        void retomarAnimacao();

        void desenharQuadro();

        void avancarParaProximoGif();
    }

    // =====================================================
    // COMPONENTES
    // =====================================================

    private final FrameLayout areaGif;

    private final Host host;

    // =====================================================
    // GERENCIADORES INTERNOS
    // =====================================================

    private final GerenciadorControleMultitoque
            gerenciadorControle;

    private final GerenciadorControleRemoto
            gerenciadorControleRemoto;

    private final GerenciadorMovimento
            gerenciadorMovimento;

    private final GerenciadorReproducaoGif
            gerenciadorReproducao;

    // =====================================================
    // CONEXÃO
    // =====================================================

    private ServicoConexao servicoConexao;

    private boolean modoIndividual;

    // =====================================================
    // ESTADO
    // =====================================================

    private boolean controleRemotoAtivo = false;

    private boolean encerrado = false;

    // =====================================================
    // CONSTRUTOR
    // =====================================================

    public CoordenadorInteracao(
            FrameLayout areaGif,
            TextView controleEsquerdo,
            TextView controleDireito,
            GerenciadorReproducaoGif gerenciadorReproducao,
            Host host,
            boolean modoIndividual) {

        if (areaGif == null ||
                controleEsquerdo == null ||
                controleDireito == null ||
                gerenciadorReproducao == null ||
                host == null) {

            throw new IllegalArgumentException(
                    "Os componentes obrigatórios não podem ser nulos."
            );
        }

        this.areaGif = areaGif;
        this.gerenciadorReproducao = gerenciadorReproducao;
        this.host = host;
        this.modoIndividual = modoIndividual;

        gerenciadorControle =
                new GerenciadorControleMultitoque(
                        areaGif,
                        controleEsquerdo,
                        controleDireito,
                        this
                );

        gerenciadorControleRemoto =
                new GerenciadorControleRemoto(this);

        gerenciadorMovimento =
                new GerenciadorMovimento(this);
    }

    // =====================================================
    // CONFIGURAÇÃO
    // =====================================================

    public void configurar() {

        if (!podeInteragir()) {
            return;
        }

        gerenciadorControle.configurar();

        Log.d(
                TAG,
                "Interação configurada."
        );
    }

    // =====================================================
    // CONFIGURAÇÃO DA CONEXÃO
    // =====================================================

    public void definirServicoConexao(
            ServicoConexao servicoConexao) {

        if (encerrado) {
            return;
        }

        this.servicoConexao = servicoConexao;
    }

    // =====================================================
    // CONFIGURAÇÃO DO MODO DE EXECUÇÃO
    // =====================================================

    public void definirModoIndividual(
            boolean modoIndividual) {

        if (encerrado) {
            return;
        }

        this.modoIndividual = modoIndividual;
    }

    // =====================================================
    // RECEBER CONTROLE REMOTO
    // =====================================================

    public void processarControleRemoto(
            JSONObject dados) {

        if (!podeInteragir() || dados == null) {
            return;
        }

        gerenciadorControleRemoto.processar(dados);
    }

    // =====================================================
    // PROCESSAMENTO CENTRAL DO PROGRESSO
    // =====================================================

    private void processarProgresso(
            float progresso,
            boolean origemManual) {

        if (!podeInteragir() ||
                !host.possuiGif()) {
            return;
        }

        if (Float.isNaN(progresso) ||
                Float.isInfinite(progresso)) {
            return;
        }

        progresso = Math.max(
                0f,
                Math.min(1f, progresso)
        );

        // Registrar o movimento.

        gerenciadorMovimento.registrarProgresso(
                progresso,
                origemManual &&
                        gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );

        // Atualizar a posição temporal do GIF.

        int duracao = Math.max(
                1,
                host.obterDuracaoGif()
        );

        gerenciadorReproducao.definirPosicao(
                (long) (progresso * duracao)
        );

        // Atualizar a imagem.

        host.desenharQuadro();

        // Somente os movimentos manuais
        // precisam ser enviados ao parceiro.

        if (origemManual) {

            enviarComandoControle(
                    true,
                    progresso
            );
        }
    }

    // =====================================================
    // CONTROLE REMOTO RECEBIDO
    // =====================================================

    @Override
    public void onControleRecebido(float progresso) {

        if (!podeInteragir() ||
                !host.possuiGif()) {
            return;
        }

        controleRemotoAtivo = true;

        host.pausarAnimacao();

        // O controle remoto atualiza os botões
        // na thread principal.

        final float progressoFinal = progresso;

        areaGif.post(() -> {

            if (!podeInteragir() ||
                    !host.possuiGif()) {
                return;
            }

            gerenciadorControle.moverControles(
                    progressoFinal
            );

            processarProgresso(
                    progressoFinal,
                    false
            );
        });
    }

    // =====================================================
    // CONTROLE REMOTO LIBERADO
    // =====================================================

    @Override
    public void onControleLiberado() {

        if (encerrado) {
            return;
        }

        controleRemotoAtivo = false;

        Log.d(
                TAG,
                "Controle remoto liberado."
        );

        finalizarMovimentoSeLiberado();
    }

    // =====================================================
    // CONTROLE MANUAL INICIADO
    // =====================================================

    @Override
    public void onControleManualIniciado() {

        if (!podeInteragir() ||
                !host.possuiGif()) {
            return;
        }

        host.pausarAnimacao();
    }

    // =====================================================
    // PROGRESSO MANUAL
    // =====================================================

    @Override
    public void onProgressoManual(float progresso) {

        processarProgresso(
                progresso,
                true
        );
    }

    // =====================================================
    // CONTROLE MANUAL LIBERADO
    // =====================================================

    @Override
    public void onControleManualLiberado() {

        if (encerrado) {
            return;
        }

        enviarComandoControle(
                false,
                0f
        );

        finalizarMovimentoSeLiberado();
    }

    @Override
    public void onSolicitacaoTrocaIniciada() {
    }

    @Override
    public void onSolicitacaoTrocaCancelada() {
    }

    @Override
    public void onSolicitacaoTrocaConcluida() {
    }

    // =====================================================
    // ENVIO DE COMANDOS AO PARCEIRO
    // =====================================================

    public void enviarComandoControle(
            boolean ativo,
            float progresso) {

        if (encerrado) {
            return;
        }

        gerenciadorControleRemoto.enviar(
                servicoConexao,
                modoIndividual,
                ativo,
                progresso
        );
    }

    // =====================================================
    // FINALIZAÇÃO DOS MOVIMENTOS
    // =====================================================

    public void finalizarMovimentoSeLiberado() {

        if (encerrado) {
            return;
        }

        gerenciadorMovimento.finalizarMovimentoSeLiberado(
                gerenciadorControle.isControleManualAtivo(),
                controleRemotoAtivo
        );
    }

    // =====================================================
    // PREPARAÇÃO PARA UM NOVO GIF
    // =====================================================

    public void prepararParaNovoGif() {

        if (encerrado) {
            return;
        }

        boolean haviaControleAtivo =
                controleRemotoAtivo ||
                        gerenciadorControle.isControleManualAtivo();

        // Informar ao parceiro que o controle foi liberado.

        if (haviaControleAtivo) {

            enviarComandoControle(
                    false,
                    0f
            );
        }

        // Limpar estados de interação.

        controleRemotoAtivo = false;

        gerenciadorControle.limpar();

        gerenciadorMovimento.reiniciarContagem();

        // Reposicionar os controles no centro.

        if (podeInteragir()) {

            gerenciadorControle.moverControles(
                    0.5f
            );
        }

        Log.d(
                TAG,
                "Controles preparados para o próximo GIF."
        );
    }

    // =====================================================
    // REINICIAR CONTAGEM
    // =====================================================

    public void reiniciarContagem() {

        if (encerrado) {
            return;
        }

        gerenciadorMovimento.reiniciarContagem();
    }

    // =====================================================
    // MOVER CONTROLES
    // =====================================================

    public void moverControles(float progresso) {

        if (!podeInteragir()) {
            return;
        }

        gerenciadorControle.moverControles(
                progresso
        );
    }

    // =====================================================
    // INTERFACE DO GERENCIADOR DE MOVIMENTO
    // =====================================================

    @Override
    public boolean possuiGif() {

        return !encerrado &&
                host.possuiGif();
    }

    @Override
    public void avancarParaProximoGif() {

        if (!podeInteragir() ||
                !host.possuiGif()) {
            return;
        }

        host.avancarParaProximoGif();
    }

    @Override
    public void retomarAnimacao() {

        if (!podeInteragir() ||
                !host.possuiGif()) {
            return;
        }

        host.retomarAnimacao();
    }

    // =====================================================
    // CONSULTAS DE ESTADO
    // =====================================================

    public boolean isControleRemotoAtivo() {

        return controleRemotoAtivo;
    }

    public boolean isControleManualAtivo() {

        return gerenciadorControle.isControleManualAtivo();
    }

    public boolean isEsquerdoPressionado() {

        return gerenciadorControle.isEsquerdoPressionado();
    }

    public boolean isDireitoPressionado() {

        return gerenciadorControle.isDireitoPressionado();
    }

    public int getCiclosConcluidos() {

        return gerenciadorMovimento.getCiclosConcluidos();
    }

    public int getCiclosNecessarios() {

        return gerenciadorMovimento.getCiclosNecessarios();
    }

    // =====================================================
    // ACESSO COMPATÍVEL AOS GERENCIADORES
    // =====================================================

    /**
     * Mantido temporariamente para compatibilidade
     * com os módulos que ainda utilizam esta referência.
     *
     * A intenção é eliminar esse acesso direto
     * nas próximas etapas da modularização.
     */
    public GerenciadorMovimento getGerenciadorMovimento() {

        return gerenciadorMovimento;
    }

    /**
     * Mantido temporariamente para compatibilidade
     * com os módulos que ainda utilizam esta referência.
     */
    public GerenciadorControleMultitoque getGerenciadorControle() {

        return gerenciadorControle;
    }

    // =====================================================
    // DISPONIBILIDADE
    // =====================================================

    private boolean podeInteragir() {

        return !encerrado &&
                host.atividadeDisponivel();
    }

    // =====================================================
    // ENCERRAMENTO
    // =====================================================

    public void encerrar() {

        if (encerrado) {
            return;
        }

        encerrado = true;

        controleRemotoAtivo = false;

        gerenciadorControle.limpar();

        gerenciadorMovimento.reiniciarContagem();

        servicoConexao = null;

        Log.d(
                TAG,
                "Coordenador de interação encerrado."
        );
    }

    // =====================================================
    // CONSULTA DE ENCERRAMENTO
    // =====================================================

    public boolean estaEncerrado() {

        return encerrado;
    }
}