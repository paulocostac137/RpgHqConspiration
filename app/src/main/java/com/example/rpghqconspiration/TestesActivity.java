package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TestesActivity extends AppCompatActivity {

    private static final String TAG = "TestesActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private final ArrayList<String> nomes = new ArrayList<>();
    private final ArrayList<String> links = new ArrayList<>();

    private TextView status;
    private ListView listaGifs;
    private ImageView imagem;
    private Button botaoReproduzir;
    private GifRepository gifRepository;
    private GifFrameSequence sequencia;
    private int indiceSelecionado = -1;
    private int ultimoQuadroDesenhado = -1;
    private long geracaoSelecao;
    private long inicioCicloMs;
    private boolean carregando;
    private boolean baixandoLista;

    private final Runnable atualizarCiclo = new Runnable() {
        @Override
        public void run() {
            if (sequencia == null || carregando || isFinishing()) {
                return;
            }

            long decorrido = SystemClock.uptimeMillis() - inicioCicloMs;
            long duracao = sequencia.getDurationMs();
            if (decorrido >= duracao) {
                mostrarQuadro(sequencia.getFrameCount() - 1);
                status.setText(
                        "Ciclo concluído: " +
                                sequencia.getFrameCount() + " quadros."
                );
                return;
            }

            int indice = (int) Math.min(
                    sequencia.getFrameCount() - 1L,
                    decorrido * sequencia.getFrameCount() / duracao
            );
            mostrarQuadro(indice);
            long duracaoQuadro = Math.max(
                    1L,
                    duracao / sequencia.getFrameCount()
            );
            handler.postDelayed(
                    this,
                    Math.min(30L, duracaoQuadro)
            );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testes);

        status = findViewById(R.id.statusTestes);
        listaGifs = findViewById(R.id.listaGifs);
        imagem = findViewById(R.id.imagemCicloGif);
        botaoReproduzir = findViewById(R.id.botaoReproduzir);
        findViewById(R.id.botaoVoltar).setOnClickListener(view -> finish());
        botaoReproduzir.setOnClickListener(view -> reproduzirCiclo());

        gifRepository = new GifRepository(
                new File(getFilesDir(), "gifs")
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                nomes
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent) {
                TextView item = (TextView) super.getView(
                        position,
                        convertView,
                        parent
                );
                item.setTextColor(0xFFFFFFFF);
                item.setGravity(Gravity.CENTER_VERTICAL);
                item.setPadding(16, 12, 16, 12);
                return item;
            }
        };
        listaGifs.setAdapter(adapter);
        listaGifs.setOnItemClickListener((parent, view, position, id) ->
                carregarGif(position)
        );

        carregarPlanilha(adapter);
    }

    private void carregarPlanilha(ArrayAdapter<String> adapter) {
        status.setText("Lendo a planilha...");
        executor.execute(() -> {
            try {
                String csv = new PlanilhaRepository().baixarConteudo();
                ArrayList<String> nomesBaixados = new ArrayList<>();
                ArrayList<String> linksBaixados = new ArrayList<>();
                ArrayList<String> descricoes = new ArrayList<>();
                PlanilhaParser.preencherListas(
                        csv,
                        nomesBaixados,
                        linksBaixados,
                        descricoes
                );
                handler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    nomes.addAll(nomesBaixados);
                    links.addAll(linksBaixados);
                    adapter.notifyDataSetChanged();
                    if (nomes.isEmpty()) {
                        status.setText("A planilha não contém GIFs.");
                        return;
                    }
                    baixarGifsDaPlanilha();
                });
            } catch (Exception erro) {
                handler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        status.setText(
                                "Erro ao carregar a planilha: " +
                                        erro.getMessage()
                        );
                    }
                });
            }
        });
    }

    private void baixarGifsDaPlanilha() {
        baixandoLista = true;
        listaGifs.setEnabled(false);
        status.setText("Baixando GIF 1 de " + links.size() + "...");
        executor.execute(() -> {
            ArrayList<String> falhas = new ArrayList<>();
            for (int i = 0; i < links.size(); i++) {
                try {
                    gifRepository.obter(links.get(i));
                } catch (Exception erro) {
                    falhas.add(nomes.get(i));
                    Log.e(
                            TAG,
                            "Falha ao baixar " + nomes.get(i),
                            erro
                    );
                }

                int concluidos = i + 1;
                handler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        status.setText(
                                "Baixando GIF " + concluidos +
                                        " de " + links.size() + "..."
                        );
                    }
                });
            }

            ArrayList<String> falhasFinais = new ArrayList<>(falhas);
            handler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                baixandoLista = false;
                listaGifs.setEnabled(true);
                if (falhasFinais.isEmpty()) {
                    status.setText(
                            links.size() +
                                    " GIFs baixados. Selecione um para ver um ciclo."
                    );
                } else {
                    status.setText(
                            "Baixados " + (links.size() - falhasFinais.size())
                                    + " de " + links.size() +
                                    ". Falhas: " + falhasFinais.size() +
                                    ". Toque em um item para tentar novamente."
                    );
                }
            });
        });
    }

    private void carregarGif(int indice) {
        if (carregando || baixandoLista ||
                indice < 0 || indice >= links.size()) {
            return;
        }

        pararCiclo();
        carregando = true;
        indiceSelecionado = indice;
        ultimoQuadroDesenhado = -1;
        imagem.setImageDrawable(null);
        liberarSequencia();
        botaoReproduzir.setEnabled(false);
        status.setText("Baixando e extraindo um ciclo de " +
                nomes.get(indice) + "...");

        long geracao = ++geracaoSelecao;
        String link = links.get(indice);
        executor.execute(() -> {
            try {
                File arquivo = gifRepository.obter(link);
                GifFrameSequence extraida =
                        GifDecoder.carregarSequencia(arquivo);
                handler.post(() -> {
                    if (isFinishing() || isDestroyed() ||
                            geracao != geracaoSelecao) {
                        extraida.liberar();
                        return;
                    }

                    carregando = false;
                    sequencia = extraida;
                    botaoReproduzir.setEnabled(true);
                    reproduzirCiclo();
                });
            } catch (Exception erro) {
                handler.post(() -> {
                    if (isFinishing() || isDestroyed() ||
                            geracao != geracaoSelecao) {
                        return;
                    }
                    carregando = false;
                    status.setText(
                            "Erro ao baixar ou preparar o GIF: " +
                                    erro.getMessage()
                    );
                });
            }
        });
    }

    private void reproduzirCiclo() {
        if (sequencia == null || carregando) {
            return;
        }

        pararCiclo();
        inicioCicloMs = SystemClock.uptimeMillis();
        ultimoQuadroDesenhado = -1;
        status.setText(
                nomes.get(indiceSelecionado) + " | reproduzindo um ciclo..."
        );
        atualizarCiclo.run();
    }

    private void mostrarQuadro(int indice) {
        if (sequencia == null || indice == ultimoQuadroDesenhado) {
            return;
        }

        ultimoQuadroDesenhado = indice;
        imagem.setImageBitmap(sequencia.getFrame(indice));
    }

    private void pararCiclo() {
        handler.removeCallbacks(atualizarCiclo);
    }

    private void liberarSequencia() {
        if (sequencia != null) {
            sequencia.liberar();
            sequencia = null;
        }
    }

    @Override
    protected void onDestroy() {
        geracaoSelecao++;
        pararCiclo();
        imagem.setImageDrawable(null);
        liberarSequencia();
        executor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
