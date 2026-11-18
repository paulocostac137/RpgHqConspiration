package com.example.rpghqconspiration;

import android.widget.ImageView;

public class GifFrameRenderer {

    private final ImageView imagemGif;

    public GifFrameRenderer(ImageView imagemGif) {
        this.imagemGif = imagemGif;
    }

    public void desenhar(GifFrameSequence sequencia, int indiceQuadro) {
        if (sequencia == null || imagemGif == null) {
            return;
        }

        imagemGif.setImageBitmap(sequencia.getFrame(indiceQuadro));
        imagemGif.invalidate();
    }

    public void liberar() {
        if (imagemGif != null) {
            imagemGif.setImageDrawable(null);
        }
    }
}
