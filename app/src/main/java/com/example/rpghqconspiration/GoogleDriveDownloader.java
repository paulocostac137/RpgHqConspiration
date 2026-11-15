package com.example.rpghqconspiration;

import android.graphics.Movie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Responsável por baixar e validar arquivos GIF
 * armazenados no Google Drive.
 *
 * Não manipula Views nem controla a navegação.
 */
public final class GoogleDriveDownloader {

    private GoogleDriveDownloader() {
        // Impede a criação de instâncias.
    }

    /**
     * Baixa um GIF e retorna o arquivo salvo.
     *
     * @param id ID do arquivo no Google Drive.
     * @param pastaGifs Pasta onde os GIFs são armazenados.
     */
    public static File baixar(
            String id,
            File pastaGifs) throws Exception {

        if (id == null || id.trim().isEmpty()) {
            throw new Exception("ID do arquivo inválido.");
        }

        if (pastaGifs == null) {
            throw new Exception("Pasta de GIFs inválida.");
        }

        if (!pastaGifs.exists() && !pastaGifs.mkdirs()) {
            throw new Exception("Não foi possível criar a pasta de GIFs.");
        }

        File arquivo = new File(
                pastaGifs,
                id + ".gif"
        );

        File temporario = new File(
                pastaGifs,
                id + ".part"
        );

        HttpURLConnection conexao = null;

        try {

            String endereco =
                    "https://drive.usercontent.google.com/download?id=" +
                            id +
                            "&export=download&confirm=t";

            conexao = (HttpURLConnection)
                    new URL(endereco).openConnection();

            conexao.setConnectTimeout(20000);
            conexao.setReadTimeout(30000);
            conexao.setInstanceFollowRedirects(true);

            int codigoHttp = conexao.getResponseCode();

            if (codigoHttp != HttpURLConnection.HTTP_OK) {
                throw new Exception("Erro HTTP: " + codigoHttp);
            }

            try (
                    InputStream entrada = conexao.getInputStream();

                    FileOutputStream saida =
                            new FileOutputStream(temporario)
            ) {

                byte[] buffer = new byte[8192];

                int quantidade;

                while ((quantidade = entrada.read(buffer)) != -1) {
                    saida.write(buffer, 0, quantidade);
                }

                saida.flush();
            }

            if (temporario.length() == 0) {
                throw new Exception("Arquivo vazio.");
            }

            try (
                    FileInputStream entradaValidacao =
                            new FileInputStream(temporario)
            ) {

                Movie teste = Movie.decodeStream(
                        entradaValidacao
                );

                if (teste == null) {
                    throw new Exception(
                            "O arquivo recebido não é um GIF válido."
                    );
                }
            }

            if (arquivo.exists() && !arquivo.delete()) {
                throw new Exception(
                        "Não foi possível substituir o arquivo antigo."
                );
            }

            if (!temporario.renameTo(arquivo)) {
                throw new Exception("Erro ao salvar GIF.");
            }

            return arquivo;

        } catch (Exception e) {

            if (temporario.exists()) {
                temporario.delete();
            }

            throw e;

        } finally {

            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }
}