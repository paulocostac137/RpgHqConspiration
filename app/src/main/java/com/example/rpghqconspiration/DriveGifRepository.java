package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public final class DriveGifRepository implements GifDownloadSource {

    private static final String TAG = "DriveGifRepository";

    public interface Callback {
        void onSuccess(File arquivo);
        void onError(String mensagem, Exception erro);
    }

    public DriveGifRepository() {
    }

    @Override
    public String extrairId(String link) {

        try {

            if (link.contains("/file/d/")) {
                String parte = link.split("/file/d/")[1];
                return parte.split("/")[0];
            }

            if (link.contains("id=")) {
                String parte = link.split("id=")[1];
                return parte.split("&")[0];
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void baixarSeNecessario(File pastaGifs,
                                  GifItem item,
                                  Handler handler,
                                  ExecutorService executor,
                                  GifDownloadSource.Callback callback) {

        String link = item != null ? item.getLink() : "";
        String id = extrairId(link);

        if (id == null) {
            handler.post(() -> callback.onError(
                    "Link inválido.",
                    new IllegalArgumentException("Link inválido")
            ));
            return;
        }

        File arquivo = new File(pastaGifs, id + ".gif");

        if (arquivo.exists() && arquivo.length() > 0) {
            handler.post(() -> callback.onSuccess(arquivo));
            return;
        }

        executor.execute(() -> {

            HttpURLConnection conexao = null;
            File temporario = new File(pastaGifs, id + ".part");

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

                if (conexao.getResponseCode() != 200) {
                    throw new Exception("Erro HTTP: " + conexao.getResponseCode());
                }

                try (
                        InputStream entrada = conexao.getInputStream();
                        FileOutputStream saida = new FileOutputStream(temporario)
                ) {
                    byte[] buffer = new byte[8192];
                    int quantidade;

                    while ((quantidade = entrada.read(buffer)) != -1) {
                        saida.write(buffer, 0, quantidade);
                    }
                }

                if (temporario.length() == 0) {
                    throw new Exception("Arquivo vazio.");
                }

                if (!temporario.renameTo(arquivo)) {
                    throw new Exception("Erro ao salvar GIF.");
                }

                handler.post(() -> callback.onSuccess(arquivo));

            } catch (Exception e) {

                temporario.delete();
                Log.e(TAG, "Erro no download do GIF.", e);

                handler.post(() -> callback.onError(
                        "Erro no download: " + e.getMessage(),
                        e
                ));

            } finally {
                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }

    public static void baixarSeNecessario(File pastaGifs,
                                         String link,
                                         Handler handler,
                                         ExecutorService executor,
                                         Callback callback) {

        String id = new DriveGifRepository().extrairId(link);

        if (id == null) {
            handler.post(() -> callback.onError(
                    "Link inválido.",
                    new IllegalArgumentException("Link inválido")
            ));
            return;
        }

        File arquivo = new File(pastaGifs, id + ".gif");

        if (arquivo.exists() && arquivo.length() > 0) {
            handler.post(() -> callback.onSuccess(arquivo));
            return;
        }

        executor.execute(() -> {

            HttpURLConnection conexao = null;
            File temporario = new File(pastaGifs, id + ".part");

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

                if (conexao.getResponseCode() != 200) {
                    throw new Exception("Erro HTTP: " + conexao.getResponseCode());
                }

                try (
                        InputStream entrada = conexao.getInputStream();
                        FileOutputStream saida = new FileOutputStream(temporario)
                ) {
                    byte[] buffer = new byte[8192];
                    int quantidade;

                    while ((quantidade = entrada.read(buffer)) != -1) {
                        saida.write(buffer, 0, quantidade);
                    }
                }

                if (temporario.length() == 0) {
                    throw new Exception("Arquivo vazio.");
                }

                if (!temporario.renameTo(arquivo)) {
                    throw new Exception("Erro ao salvar GIF.");
                }

                handler.post(() -> callback.onSuccess(arquivo));

            } catch (Exception e) {

                temporario.delete();
                Log.e(TAG, "Erro no download do GIF.", e);

                handler.post(() -> callback.onError(
                        "Erro no download: " + e.getMessage(),
                        e
                ));

            } finally {
                if (conexao != null) {
                    conexao.disconnect();
                }
            }
        });
    }
}
