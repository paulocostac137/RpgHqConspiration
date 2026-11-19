package com.example.rpghqconspiration;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CriarActivity extends AppCompatActivity {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText campoLinkGif;
    private Button botaoProcessarGif;
    private TextView textoStatusProcessamento;
    private GerenciadorDecodificacaoGif gerenciadorDecodificacaoGif;
    private GifRepository gifRepository;
    private GifFrameSequence sequenciaGerada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar);
        ModoImersivo.ativar(getWindow());

        Button botaoVoltar = findViewById(R.id.botaoVoltar);
        campoLinkGif = findViewById(R.id.campoLinkGif);
        botaoProcessarGif = findViewById(R.id.botaoProcessarGif);
        textoStatusProcessamento =
                findViewById(R.id.textoStatusProcessamento);
        gerenciadorDecodificacaoGif =
                new GerenciadorDecodificacaoGif(executor, handler);
        gifRepository = new GifRepository(
                new File(getFilesDir(), "gifs")
        );

        botaoVoltar.setOnClickListener(view -> finish());
        botaoProcessarGif.setOnClickListener(
                view -> processarGifInformado()
        );
    }

    private void processarGifInformado() {
        String link = campoLinkGif.getText().toString().trim();
        if (link.isEmpty()) {
            campoLinkGif.setError("Informe o link do GIF.");
            campoLinkGif.requestFocus();
            return;
        }

        if (GoogleDriveUtils.extrairId(link) == null) {
            campoLinkGif.setError(
                    "Informe um link válido de arquivo do Google Drive."
            );
            campoLinkGif.requestFocus();
            return;
        }

        ocultarTeclado();
        botaoProcessarGif.setEnabled(false);
        campoLinkGif.setEnabled(false);
        textoStatusProcessamento.setText("Baixando e preparando o GIF...");

        if (sequenciaGerada != null) {
            sequenciaGerada.liberar();
            sequenciaGerada = null;
        }

        gerenciadorDecodificacaoGif.prepararLinhaDoTempo(
                Collections.singletonList(link),
                gifRepository,
                new GerenciadorDecodificacaoGif.TimelineListener() {
                    @Override
                    public void onTimelineReady(
                            GifFrameSequence timeline) {
                        if (isFinishing() || isDestroyed()) {
                            timeline.liberar();
                            return;
                        }

                        sequenciaGerada = timeline;
                        ResultadoProcessamentoGif.publicar(timeline);
                        sequenciaGerada = null;
                        startActivity(new Intent(
                                CriarActivity.this,
                                ResultadoGifActivity.class
                        ));
                    }

                    @Override
                    public void onErro(Exception erro) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        textoStatusProcessamento.setText(
                                "Não foi possível processar o GIF: " +
                                        erro.getMessage()
                        );
                        liberarFormulario();
                    }
                }
        );
    }

    private void liberarFormulario() {
        botaoProcessarGif.setEnabled(true);
        campoLinkGif.setEnabled(true);
    }

    private void ocultarTeclado() {
        InputMethodManager teclado = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (teclado != null) {
            teclado.hideSoftInputFromWindow(
                    campoLinkGif.getWindowToken(),
                    0
            );
        }
        campoLinkGif.clearFocus();
    }

    @Override
    protected void onDestroy() {
        if (sequenciaGerada != null) {
            sequenciaGerada.liberar();
            sequenciaGerada = null;
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
