package com.example.rpghqconspiration;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MarcadoresPlanilhaRepository {

    private static final String TAG = "MarcadoresPlanilha";
    private static final String WEB_APP_URL =
            "https://script.google.com/macros/s/AKfycbxKuZEsuSILIccEAGDrPikd4CZdD4XIZSrg-j_mjkx7MaEJ4iYIPZSEk6ea-5ke4EJA/exec";
    private static final String TOKEN = "9718";
    private static final int TIMEOUT_MS = 20000;

    private MarcadoresPlanilhaRepository() {
    }

    public static void enviar(List<String[]> linhas) throws Exception {
        if (WEB_APP_URL.startsWith("COLE_")) {
            throw new IllegalStateException(
                    "Configure a URL do Apps Script Web App no app."
            );
        }
        if (linhas == null || linhas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não há marcadores para salvar."
            );
        }

        JSONArray registros = new JSONArray();
        for (String[] linha : linhas) {
            if (linha == null || linha.length != 4) {
                throw new IllegalArgumentException(
                        "Um dos registros de marcador está inválido."
                );
            }
            JSONArray registro = new JSONArray();
            for (String valor : linha) {
                registro.put(valor == null ? "" : valor);
            }
            registros.put(registro);
        }

        JSONObject corpo = new JSONObject();
        corpo.put("token", TOKEN);
        corpo.put("rows", registros);

        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(WEB_APP_URL)
                    .openConnection();
            conexao.setRequestMethod("POST");
            conexao.setConnectTimeout(TIMEOUT_MS);
            conexao.setReadTimeout(TIMEOUT_MS);
            conexao.setDoOutput(true);
            conexao.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
            );

            byte[] dados = corpo.toString().getBytes(StandardCharsets.UTF_8);
            conexao.setFixedLengthStreamingMode(dados.length);
            try (OutputStream saida = conexao.getOutputStream()) {
                saida.write(dados);
            }

            int codigo = conexao.getResponseCode();
            InputStream resposta = codigo >= 200 && codigo < 300
                    ? conexao.getInputStream()
                    : conexao.getErrorStream();
            String textoResposta = lerResposta(resposta);
            if (codigo < 200 || codigo >= 300) {
                throw new IllegalStateException(
                        "Erro HTTP " + codigo + ": " + textoResposta
                );
            }

            JSONObject resultado = new JSONObject(textoResposta);
            if (!resultado.optBoolean("ok", false)) {
                throw new IllegalStateException(
                        resultado.optString(
                                "error",
                                "O Apps Script não confirmou a gravação."
                        )
                );
            }
        } catch (Exception erro) {
            Log.e(TAG, "Falha ao salvar marcadores na planilha.", erro);
            throw erro;
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    private static String lerResposta(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder resposta = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                resposta.append(linha);
            }
        }
        return resposta.toString();
    }
}
