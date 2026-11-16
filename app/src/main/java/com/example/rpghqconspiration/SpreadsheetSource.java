package com.example.rpghqconspiration;

import android.os.Handler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public interface SpreadsheetSource {

    void carregarPlanilha(Handler handler,
                          ExecutorService executor,
                          Callback callback);

    interface Callback {
        void onSuccess(ArrayList<GifItem> itens);
        void onError(String mensagem, Exception erro);
    }
}
