package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultadoGifActivity extends AppCompatActivity {

    private static final long QUADROS_POR_SEGUNDO = 30L;

    private GifFrameSequence sequencia;
    private ImageView imagemQuadro;
    private Button botaoQuadroAnterior;
    private Button botaoProximoQuadro;
    private Button botaoPlayQuadros;
    private SeekBar barraQuadros;
    private TextView textoContadorQuadros;
    private MarcadoresBarraView marcadoresBarraQuadros;
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
        textoContadorQuadros = findViewById(R.id.textoContadorQuadros);
        imagemQuadro = findViewById(R.id.imagemQuadroResultado);
        botaoQuadroAnterior = findViewById(R.id.botaoQuadroAnterior);
        botaoProximoQuadro = findViewById(R.id.botaoProximoQuadro);
        botaoPlayQuadros = findViewById(R.id.botaoPlayQuadros);
        barraQuadros = findViewById(R.id.barraQuadros);
        marcadoresBarraQuadros =
                findViewById(R.id.marcadoresBarraQuadros);
        Button botaoGo = findViewById(R.id.botaoGo);
        Button botaoOn = findViewById(R.id.botaoOn);
        Button botaoOut = findViewById(R.id.botaoOut);
        botaoVoltar.setOnClickListener(view -> finish());

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
                view -> marcadoresBarraQuadros.adicionarMarcador(
                        indiceQuadro,
                        "G"
                )
        );
        botaoOn.setOnClickListener(
                view -> marcadoresBarraQuadros.adicionarMarcador(
                        indiceQuadro,
                        "O"
                )
        );
        botaoOut.setOnClickListener(
                view -> marcadoresBarraQuadros.adicionarMarcador(
                        indiceQuadro,
                        "T"
                )
        );
        mostrarQuadro(0);
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
