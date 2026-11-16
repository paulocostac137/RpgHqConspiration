package com.example.rpghqconspiration;

import android.os.Handler;

import java.io.File;
import java.util.concurrent.ExecutorService;

public interface GifDownloadSource {

    void baixarSeNecessario(File pastaGifs,
                            GifItem item,
                            Handler handler,
                            ExecutorService executor,
                            Callback callback);

    String extrairId(String link);

    interface Callback {
        void onSuccess(File arquivo);
        void onError(String mensagem, Exception erro);
    }
}
