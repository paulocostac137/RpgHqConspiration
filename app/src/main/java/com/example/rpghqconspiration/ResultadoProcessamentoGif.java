package com.example.rpghqconspiration;

public final class ResultadoProcessamentoGif {

    private static GifFrameSequence sequenciaPendente;
    private static GifFrameSequence sequenciaPreview;

    private ResultadoProcessamentoGif() {
    }

    public static synchronized void publicar(GifFrameSequence sequencia) {
        if (sequencia == null) {
            throw new IllegalArgumentException(
                    "A sequência processada não pode ser nula."
            );
        }
        if (sequenciaPendente != null) {
            sequenciaPendente.liberar();
        }
        sequenciaPendente = sequencia;
    }

    public static synchronized GifFrameSequence consumir() {
        GifFrameSequence sequencia = sequenciaPendente;
        sequenciaPendente = null;
        return sequencia;
    }

    public static synchronized void publicarPreview(
            GifFrameSequence sequencia) {
        if (sequencia == null) {
            throw new IllegalArgumentException(
                    "A sequência de preview não pode ser nula."
            );
        }
        limparPreview();
        sequenciaPreview = sequencia;
    }

    public static synchronized GifFrameSequence consumirPreview() {
        GifFrameSequence sequencia = sequenciaPreview;
        sequenciaPreview = null;
        return sequencia;
    }

    public static synchronized void limparPreview() {
        if (sequenciaPreview != null) {
            sequenciaPreview.liberar();
            sequenciaPreview = null;
        }
    }
}
