package com.example.rpghqconspiration;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultadoGifActivity extends AppCompatActivity {

    private static final long QUADROS_POR_SEGUNDO = 30L;

    private GifFrameSequence sequencia;
    private ImageView imagemQuadro;
    private Button botaoQuadroAnterior;
    private Button botaoProximoQuadro;
    private Button botaoPlayQuadros;
    private Button botaoGo;
    private Button botaoOn;
    private Button botaoOut;
    private SeekBar barraQuadros;
    private TextView textoContadorQuadros;
    private MarcadoresBarraView marcadoresBarraQuadros;
    private Button botaoFinalizar;
    private EditText campoTextoImagem;
    private LinearLayout controlesMarcadores;
    private LinearLayout controlesNavegacao;
    private Integer controleAplicado;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private String linkGif;
    private int indiceQuadro;
    private boolean reproduzindo;
    private long inicioReproducaoMs;
    private int quadroInicialReproducao;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable atualizarReproducao = new Runnable() {
        @Override
        public void run() {
            if (!reproduzindo || sequencia == null) {
                return;
            }

            long decorridoMs =
                    SystemClock.uptimeMillis() - inicioReproducaoMs;
            long quadrosDecorridos =
                    decorridoMs * QUADROS_POR_SEGUNDO / 1000L;
            int proximoIndice = (int) (
                    (quadroInicialReproducao + quadrosDecorridos)
                            % sequencia.getFrameCount()
            );
            mostrarQuadro(proximoIndice);

            long proximaAtualizacaoMs =
                    inicioReproducaoMs
                            + ((quadrosDecorridos + 1L) * 1000L
                            + QUADROS_POR_SEGUNDO - 1L)
                            / QUADROS_POR_SEGUNDO;
            handler.postAtTime(this, proximaAtualizacaoMs);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultado_gif);
        ModoImersivo.ativar(getWindow());

        Button botaoVoltar = findViewById(R.id.botaoVoltarResultado);
        Button botaoVer = findViewById(R.id.botaoVer);
        botaoFinalizar = findViewById(R.id.botaoFinalizar);
        campoTextoImagem = findViewById(R.id.campoTextoImagem);
        textoContadorQuadros = findViewById(R.id.textoContadorQuadros);
        imagemQuadro = findViewById(R.id.imagemQuadroResultado);
        botaoQuadroAnterior = findViewById(R.id.botaoQuadroAnterior);
        botaoProximoQuadro = findViewById(R.id.botaoProximoQuadro);
        botaoPlayQuadros = findViewById(R.id.botaoPlayQuadros);
        barraQuadros = findViewById(R.id.barraQuadros);
        marcadoresBarraQuadros =
                findViewById(R.id.marcadoresBarraQuadros);
        botaoGo = findViewById(R.id.botaoGo);
        botaoOn = findViewById(R.id.botaoOn);
        botaoOut = findViewById(R.id.botaoOut);
        controlesMarcadores =
                findViewById(R.id.controlesMarcadoresResultado);
        controlesNavegacao =
                findViewById(R.id.controlesNavegacaoResultado);
        botaoVoltar.setOnClickListener(view -> finish());
        botaoFinalizar.setOnClickListener(view -> finalizarMarcadores());
        botaoVer.setOnClickListener(view -> abrirPreview());
        configurarSeletorControle();

        linkGif = getIntent().getStringExtra("link_gif");
        sequencia = ResultadoProcessamentoGif.consumir();
        if (sequencia == null || sequencia.getFrameCount() == 0) {
            textoContadorQuadros.setText("Sem quadros");
            textoContadorQuadros.setVisibility(TextView.GONE);
            imagemQuadro.setVisibility(ImageView.GONE);
            botaoQuadroAnterior.setVisibility(Button.GONE);
            botaoProximoQuadro.setVisibility(Button.GONE);
            botaoPlayQuadros.setVisibility(Button.GONE);
            barraQuadros.setVisibility(SeekBar.GONE);
            marcadoresBarraQuadros.setVisibility(
                    MarcadoresBarraView.GONE
            );
            if (controlesNavegacao != null) {
                controlesNavegacao.setVisibility(LinearLayout.GONE);
            }
            atualizarBotoesMarcadores();
            return;
        }

        barraQuadros.setMax(sequencia.getFrameCount() - 1);
        marcadoresBarraQuadros.configurar(
                barraQuadros,
                sequencia.getFrameCount()
        );
        barraQuadros.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {
                        if (fromUser) {
                            pararReproducao();
                        }
                        mostrarQuadro(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );
        botaoQuadroAnterior.setOnClickListener(
                view -> {
                    pararReproducao();
                    mostrarQuadro(indiceQuadro - 1);
                }
        );
        botaoProximoQuadro.setOnClickListener(
                view -> {
                    pararReproducao();
                    mostrarQuadro(indiceQuadro + 1);
                }
        );
        botaoPlayQuadros.setOnClickListener(view -> {
            if (reproduzindo) {
                pararReproducao();
            } else {
                iniciarReproducao();
            }
        });
        botaoGo.setOnClickListener(
                view -> adicionarMarcadorAtual("G")
        );
        botaoOn.setOnClickListener(
                view -> adicionarMarcadorAtual("O")
        );
        botaoOut.setOnClickListener(
                view -> adicionarMarcadorAtual("T")
        );
        mostrarQuadro(0);
    }

    private void configurarSeletorControle() {
        Spinner seletorControle =
                findViewById(R.id.seletorControleResultado);
        ArrayList<String> opcoes = new ArrayList<>();
        opcoes.add("Controles...");
        for (int numero = 1; numero <= 5; numero++) {
            opcoes.add(String.format(
                    java.util.Locale.ROOT,
                    "Controle %02d",
                    numero
            ));
        }
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                opcoes
        );
        adaptador.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        seletorControle.setAdapter(adaptador);
        seletorControle.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {
                        controleAplicado =
                                position >= 1 && position <= 5
                                        ? position
                                        : null;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        controleAplicado = null;
                    }
                }
        );
    }

    private void adicionarMarcadorAtual(String letra) {
        marcadoresBarraQuadros.adicionarMarcador(indiceQuadro, letra);
        atualizarBotoesMarcadores();
    }

    private void atualizarBotoesMarcadores() {
        if (marcadoresBarraQuadros == null ||
                botaoGo == null || botaoOn == null || botaoOut == null) {
            return;
        }
        botaoGo.setEnabled(
                marcadoresBarraQuadros.podeAdicionarMarcador(
                        indiceQuadro,
                        "G"
                )
        );
        botaoOn.setEnabled(
                marcadoresBarraQuadros.podeAdicionarMarcador(
                        indiceQuadro,
                        "O"
                )
        );
        botaoOut.setEnabled(
                marcadoresBarraQuadros.podeAdicionarMarcador(
                        indiceQuadro,
                        "T"
                )
        );
        botaoOn.setText(marcadoresBarraQuadros.obterRotuloBotaoOn());
    }

    private void iniciarReproducao() {
        if (sequencia == null || sequencia.getFrameCount() < 2) {
            return;
        }
        reproduzindo = true;
        inicioReproducaoMs = SystemClock.uptimeMillis();
        quadroInicialReproducao = indiceQuadro;
        botaoPlayQuadros.setText("Ⅱ");
        handler.removeCallbacks(atualizarReproducao);
        handler.postDelayed(
                atualizarReproducao,
                (1000L + QUADROS_POR_SEGUNDO - 1L)
                        / QUADROS_POR_SEGUNDO
        );
    }

    private void pararReproducao() {
        reproduzindo = false;
        handler.removeCallbacks(atualizarReproducao);
        if (botaoPlayQuadros != null) {
            botaoPlayQuadros.setText("▶");
        }
    }

    private void finalizarMarcadores() {
        if (linkGif == null || linkGif.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "O link do GIF não está disponível.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (!marcadoresBarraQuadros.temPeloMenosTresMarcacoes()) {
            Toast.makeText(
                    this,
                    "Complete ao menos uma sequência (Go, On e Out) antes de finalizar.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        final List<String[]> linhas;
        try {
            linhas = marcadoresBarraQuadros.obterLinhasPlanilha(
                    linkGif,
                    campoTextoImagem.getText().toString()
            );
        } catch (IllegalArgumentException erro) {
            Toast.makeText(this, erro.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (linhas.isEmpty()) {
            Toast.makeText(
                    this,
                    "Adicione ao menos um marcador antes de finalizar.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        pararReproducao();
        botaoFinalizar.setEnabled(false);
        Toast.makeText(
                this,
                "Enviando os marcadores...",
                Toast.LENGTH_SHORT
        ).show();
        executor.execute(() -> {
            try {
                MarcadoresPlanilhaRepository.enviar(linhas);
                handler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(
                            this,
                            "Marcadores salvos na planilha.",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                });
            } catch (Exception erro) {
                handler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    botaoFinalizar.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Falha ao salvar: " + erro.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void mostrarQuadro(int indice) {
        if (sequencia == null || sequencia.getFrameCount() == 0) {
            return;
        }
        indiceQuadro = Math.max(
                0,
                Math.min(sequencia.getFrameCount() - 1, indice)
        );
        imagemQuadro.setImageBitmap(sequencia.getFrame(indiceQuadro));
        textoContadorQuadros.setText(
                (indiceQuadro + 1) + "/" + sequencia.getFrameCount()
        );
        botaoQuadroAnterior.setEnabled(indiceQuadro > 0);
        botaoProximoQuadro.setEnabled(
                indiceQuadro < sequencia.getFrameCount() - 1
        );
        if (barraQuadros.getProgress() != indiceQuadro) {
            barraQuadros.setProgress(indiceQuadro);
        }
        controlesMarcadores.setVisibility(LinearLayout.VISIBLE);
        controlesNavegacao.setVisibility(LinearLayout.VISIBLE);
        marcadoresBarraQuadros.setVisibility(MarcadoresBarraView.VISIBLE);
        atualizarBotoesMarcadores();
    }

    private void abrirPreview() {
        if (sequencia == null) {
            return;
        }
        if (controleAplicado == null) {
            Toast.makeText(
                    this,
                    "Selecione um controle antes de abrir a prévia.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        GifFrameSequence preview = null;
        try {
            pararReproducao();
            preview = sequencia.criarPreview(
                    marcadoresBarraQuadros.obterFaixasRepetidas()
            );
            ResultadoProcessamentoGif.publicarPreview(preview);
            final GifFrameSequence previewCriado = preview;
            final Integer controleOverride = controleAplicado;
            executor.execute(() -> {
                try {
                    int numeroControle = controleOverride != null
                            ? controleOverride
                            : obterNumeroControleDaPlanilha(linkGif);
                    handler.post(() -> {
                        if (isFinishing() || isDestroyed()) {
                            ResultadoProcessamentoGif.limparPreview();
                            previewCriado.liberar();
                            return;
                        }
                        Intent intent = new Intent(
                                ResultadoGifActivity.this,
                                PreviewGifActivity.class
                        );
                        intent.putExtra("controle_gif", numeroControle);
                        startActivity(intent);
                    });
                } catch (Exception erro) {
                    handler.post(() -> {
                        ResultadoProcessamentoGif.limparPreview();
                        previewCriado.liberar();
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(
                                    this,
                                    "Não foi possível consultar o controle " +
                                            "do GIF na planilha: " +
                                            erro.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                }
            });
        } catch (IllegalArgumentException | IllegalStateException erro) {
            ResultadoProcessamentoGif.limparPreview();
            if (preview != null) {
                preview.liberar();
            }
            Toast.makeText(
                    this,
                    erro.getMessage(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private int obterNumeroControleDaPlanilha(String link) throws Exception {
        if (link == null || link.trim().isEmpty()) {
            return 0;
        }

        String conteudo = new PlanilhaRepository().baixarConteudo();
        ArrayList<String> nomes = new ArrayList<>();
        ArrayList<String> links = new ArrayList<>();
        ArrayList<String> textos = new ArrayList<>();
        ArrayList<Integer> repeticoes = new ArrayList<>();
        ArrayList<FaixaRepeticaoGif> faixas = new ArrayList<>();
        ArrayList<Integer> controles = new ArrayList<>();

        PlanilhaParser.preencherListas(
                conteudo,
                nomes,
                links,
                textos,
                repeticoes,
                faixas,
                controles
        );

        String idInformado = GoogleDriveUtils.extrairId(link);
        if (idInformado == null) {
            return 0;
        }
        for (int i = 0; i < links.size(); i++) {
            if (idInformado.equals(
                    GoogleDriveUtils.extrairId(links.get(i)))) {
                return controles.get(i);
            }
        }

        return 0;
    }

    @Override
    protected void onDestroy() {
        pararReproducao();
        if (imagemQuadro != null) {
            imagemQuadro.setImageDrawable(null);
        }
        if (sequencia != null) {
            sequencia.liberar();
            sequencia = null;
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ModoImersivo.ativar(getWindow());
        }
    }
}
