package com.example.rpghqconspiration;

public final class ResultadoProcessamentoGif {

    private static GifFrameSequence sequenciaPendente;

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
}
