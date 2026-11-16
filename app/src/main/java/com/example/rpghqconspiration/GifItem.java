package com.example.rpghqconspiration;

public final class GifItem {

    private final String nome;
    private final String link;
    private final String texto;

    public GifItem(String nome, String link, String texto) {
        this.nome = nome == null ? "" : nome;
        this.link = link == null ? "" : link;
        this.texto = texto == null ? "" : texto;
    }

    public String getNome() {
        return nome;
    }

    public String getLink() {
        return link;
    }

    public String getTexto() {
        return texto;
    }

    public boolean hasLink() {
        return !link.isEmpty();
    }
}
