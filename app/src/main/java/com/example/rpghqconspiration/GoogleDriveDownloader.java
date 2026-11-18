package com.example.rpghqconspiration;

import android.graphics.Movie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.util.UUID;

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

        synchronized (GoogleDriveDownloader.class) {
            if (id == null || id.trim().isEmpty()) {
                throw new Exception("ID do arquivo inválido.");
            }

            if (pastaGifs == null) {
                throw new Exception("Pasta de GIFs inválida.");
            }

            if (!pastaGifs.exists() && !pastaGifs.mkdirs()) {
                throw new Exception("Não foi possível criar a pasta de GIFs.");
            }

            String idLimpo = id.trim();
            File arquivo = new File(
                    pastaGifs,
                    idLimpo + ".gif"
            );

            if (arquivo.isFile() && arquivo.length() > 0) {
                validarGif(arquivo);
                return arquivo;
            }

            File temporario = new File(
                    pastaGifs,
                    idLimpo + "." + UUID.randomUUID() + ".part"
            );

            HttpURLConnection conexao = null;

            try {
                String endereco =
                        "https://drive.usercontent.google.com/download?id=" +
                                idLimpo +
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

                validarGif(temporario);

                moverComSeguranca(temporario, arquivo);

                if (!arquivo.isFile() || arquivo.length() <= 0) {
                    throw new Exception(
                            "A movimentação do GIF não produziu um arquivo válido."
                    );
                }

                validarGif(arquivo);
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

    private static void validarGif(File arquivo) throws Exception {
        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() <= 0) {
            throw new Exception("Arquivo GIF vazio ou inválido.");
        }

        try (
                FileInputStream entrada =
                        new FileInputStream(arquivo)
        ) {
            Movie teste = Movie.decodeStream(entrada);
            if (teste == null) {
                throw new Exception(
                        "O arquivo recebido não é um GIF válido."
                );
            }
        }
    }

    private static void moverComSeguranca(
            File origem,
            File destino
    ) throws Exception {
        if (destino.exists() && !destino.delete()) {
            throw new Exception(
                    "Não foi possível substituir o arquivo GIF antigo."
            );
        }

        try (
                FileChannel entrada =
                        new RandomAccessFile(
                                origem,
                                "r"
                        ).getChannel();
                FileChannel saida =
                        new RandomAccessFile(
                                destino,
                                "rw"
                        ).getChannel()
        ) {
            entrada.transferTo(0, entrada.size(), saida);
            saida.force(true);
        } catch (Exception e) {
            if (destino.exists()) {
                destino.delete();
            }
            throw new Exception("Erro ao salvar GIF.", e);
        }

        if (!origem.delete()) {
            throw new Exception(
                    "GIF salvo, mas não foi possível remover o arquivo temporário."
            );
        }
    }
}