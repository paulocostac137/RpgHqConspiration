package com.example.rpghqconspiration;

import java.util.ArrayList;
import java.util.List;

public final class GifCatalog {

    private final ArrayList<String> nomes = new ArrayList<>();
    private final ArrayList<String> links = new ArrayList<>();
    private final ArrayList<String> textos = new ArrayList<>();
    private final ArrayList<GifItem> itens = new ArrayList<>();

    private int indiceAtual = -1;

    public void setItems(List<GifItem> itensCarregados) {
        itens.clear();
        nomes.clear();
        links.clear();
        textos.clear();

        if (itensCarregados != null) {
            itens.addAll(itensCarregados);
            for (GifItem item : itensCarregados) {
                if (item == null) {
                    continue;
                }
                nomes.add(item.getNome());
                links.add(item.getLink());
                textos.add(item.getTexto());
            }
        }

        indiceAtual = -1;
    }

    public void setItems(List<String> nomesCarregados,
                         List<String> linksCarregados,
                         List<String> textosCarregados) {
        itens.clear();
        nomes.clear();
        links.clear();
        textos.clear();

        if (nomesCarregados != null) {
            nomes.addAll(nomesCarregados);
        }

        if (linksCarregados != null) {
            links.addAll(linksCarregados);
        }

        if (textosCarregados != null) {
            textos.addAll(textosCarregados);
        }

        for (int i = 0; i < nomes.size(); i++) {
            String link = i < links.size() ? links.get(i) : "";
            String texto = i < textos.size() ? textos.get(i) : "";
            itens.add(new GifItem(nomes.get(i), link, texto));
        }

        indiceAtual = -1;
    }

    public boolean isEmpty() {
        return nomes.isEmpty();
    }

    public int proximoIndice() {
        if (nomes.isEmpty()) {
            return -1;
        }

        indiceAtual++;
        if (indiceAtual >= nomes.size()) {
            indiceAtual = 0;
        }

        return indiceAtual;
    }

    public int getIndiceAtual() {
        return indiceAtual;
    }

    public int tamanho() {
        return nomes.size();
    }

    public String getNome(int indice) {
        if (indice < 0 || indice >= nomes.size()) {
            return "";
        }
        return nomes.get(indice);
    }

    public String getLink(int indice) {
        if (indice < 0 || indice >= links.size()) {
            return "";
        }
        return links.get(indice);
    }

    public String getTexto(int indice) {
        if (indice < 0 || indice >= textos.size()) {
            return "";
        }
        return textos.get(indice);
    }

    public GifItem getItem(int indice) {
        if (indice < 0 || indice >= itens.size()) {
            return null;
        }
        return itens.get(indice);
    }

    public void limpar() {
        itens.clear();
        nomes.clear();
        links.clear();
        textos.clear();
        indiceAtual = -1;
    }
}
