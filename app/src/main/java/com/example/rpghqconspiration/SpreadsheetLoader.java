package com.example.rpghqconspiration;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public final class SpreadsheetLoader implements SpreadsheetSource {

    private static final String TAG = "SpreadsheetLoader";
    private static final String PLANILHA_PADRAO =
            "1pJ7XtHx0DLSCjsGj0zlJycm-nzqa7a8flnLfoG7k2ks";
    private static final String GID_PADRAO = "0";

    public interface Callback {
        void onSuccess(ArrayList<String> nomes,
                       ArrayList<String> links,
                       ArrayList<String> textos);

        void onError(String mensagem, Exception erro);
    }

    public SpreadsheetLoader() {
    }

    @Override
    public void carregarPlanilha(Handler handler,
                                ExecutorService executor,
                                SpreadsheetSource.Callback callback) {
        carregar(PLANILHA_PADRAO,
                GID_PADRAO,
                handler,
                executor,
                new Callback() {
                    @Override
                    public void onSuccess(ArrayList<String> nomes,
                                           ArrayList<String> links,
                                           ArrayList<String> textos) {
                        ArrayList<GifItem> itens = new ArrayList<>();
                        int limite = Math.min(nomes.size(), links.size());
                        for (int i = 0; i < limite; i++) {
                            String texto = i < textos.size() ? textos.get(i) : "";
                            itens.add(new GifItem(nomes.get(i), links.get(i), texto));
                        }
                        handler.post(() -> callback.onSuccess(itens));
                    }

                    @Override
                    public void onError(String mensagem, Exception erro) {
                        handler.post(() -> callback.onError(mensagem, erro));
                    }
                }
        );
    }

    public static void carregar(String planilhaId,
                                String gid,
                                Handler handler,
                                ExecutorService executor,
                                Callback callback) {

        executor.execute(() -> {

            HttpURLConnection conexao = null;

            try {

                String endereco =
                        "https://docs.google.com/spreadsheets/d/" +
                                planilhaId +
                                "/export?format=csv&gid=" + gid;

                URL url = new URL(endereco);
                conexao = (HttpURLConnection) url.openConnection();
                conexao.setConnectTimeout(15000);
                conexao.setReadTimeout(15000);

                BufferedReader leitor = new BufferedReader(
                        new InputStreamReader(
                                conexao.getInputStream(),
                                "UTF-8"
                        )
                );

                String linha;
                boolean primeiraLinha = true;

                ArrayList<String> nomes = new ArrayList<>();
                ArrayList<String> links = new ArrayList<>();
                ArrayList<String> textos = new ArrayList<>();

                while ((linha = leitor.readLine()) != null) {

                    ArrayList<String> colunas = CsvUtil.separarCSV(linha);

                    if (primeiraLinha) {
                        primeiraLinha = false;
                        continue;
                    }

                    if (colunas.size() < 2) {
                        continue;
                    }

                    String nome = colunas.get(0).trim();
                    String link = colunas.get(1).trim();
                    String texto = colunas.size() > 4
                            ? colunas.get(4).trim()
                            : "";

                    if (!nome.isEmpty() && !link.isEmpty()) {
                        nomes.add(nome);
                        links.add(link);
                        textos.add(texto);
                    }
                }

                leitor.close();

                handler.post(() -> callback.onSuccess(nomes, links, textos));

            } catch (Exception e) {

                Log.e(TAG, "Erro na planilha.", e);

                handler.post(() -> callback.onError(
                        "Erro ao consultar planilha: " + e.getMessage(),
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
