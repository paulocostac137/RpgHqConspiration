package com.example.rpghqconspiration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Responsável exclusivamente pelo acesso à planilha Google Sheets.
 *
 * Faz a conexão HTTP e retorna o conteúdo CSV recebido.
 * Não manipula componentes da interface nem listas da MainActivity.
 */
public class PlanilhaRepository {

    // =====================================================
    // CONFIGURAÇÃO DA PLANILHA
    // =====================================================

    private static final String PLANILHA_ID =
            "13ESbous-qqBsQf6VU_6Rw4zb_aoBguosGXY9eh86P9M";

    private static final String GID = "0";

    private static final int TEMPO_CONEXAO_MS = 15000;

    private static final int TEMPO_LEITURA_MS = 15000;

    // =====================================================
    // CONSULTA À PLANILHA
    // =====================================================

    public String baixarConteudo() throws Exception {

        HttpURLConnection conexao = null;

        try {

            String endereco =
                    "https://docs.google.com/spreadsheets/d/" +
                            PLANILHA_ID +
                            "/export?format=csv&gid=" + GID;

            URL url = new URL(endereco);

            conexao = (HttpURLConnection)
                    url.openConnection();

            conexao.setConnectTimeout(TEMPO_CONEXAO_MS);

            conexao.setReadTimeout(TEMPO_LEITURA_MS);

            int codigoHttp = conexao.getResponseCode();

            if (codigoHttp != HttpURLConnection.HTTP_OK) {

                throw new Exception(
                        "Erro HTTP: " + codigoHttp
                );
            }

            StringBuilder conteudo = new StringBuilder();

            try (
                    BufferedReader leitor =
                            new BufferedReader(
                                    new InputStreamReader(
                                            conexao.getInputStream(),
                                            "UTF-8"
                                    )
                            )
            ) {

                String linha;

                while ((linha = leitor.readLine()) != null) {

                    conteudo.append(linha).append('\n');
                }
            }

            return conteudo.toString();

        } finally {

            if (conexao != null) {

                conexao.disconnect();
            }
        }
    }
}