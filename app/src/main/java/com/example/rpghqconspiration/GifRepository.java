package com.example.rpghqconspiration;

import java.io.File;

/**
 * Responsável pelo acesso aos arquivos GIF locais
 * e pela obtenção de GIFs no Google Drive.
 *
 * Não manipula Views, Activities ou animações.
 */
public final class GifRepository {

    private final File pastaGifs;

    public GifRepository(File pastaGifs) {

        if (pastaGifs == null) {
            throw new IllegalArgumentException(
                    "A pasta de GIFs não pode ser nula."
            );
        }

        this.pastaGifs = pastaGifs;

        // Garante que a pasta de GIFs exista.
        if (!this.pastaGifs.exists()) {
            boolean criada = this.pastaGifs.mkdirs();

            if (!criada && !this.pastaGifs.exists()) {
                throw new IllegalStateException(
                        "Não foi possível criar a pasta de GIFs."
                );
            }
        }

        if (!this.pastaGifs.isDirectory()) {
            throw new IllegalStateException(
                    "O caminho informado não é uma pasta válida."
            );
        }
    }

    /**
     * Procura um GIF já armazenado localmente.
     *
     * Retorna null quando o arquivo ainda não existe
     * ou está vazio.
     */
    public File localizar(String link) throws Exception {

        String id = obterId(link);

        File arquivo = new File(
                pastaGifs,
                id + ".gif"
        );

        if (arquivo.isFile() && arquivo.length() > 0) {
            return arquivo;
        }

        return null;
    }

    /**
     * Baixa um GIF do Google Drive.
     */
    public File baixar(String link) throws Exception {

        String id = obterId(link);

        File arquivo = GoogleDriveDownloader.baixar(
                id,
                pastaGifs
        );

        validarArquivo(arquivo);

        return arquivo;
    }

    /**
     * Obtém um GIF local ou realiza seu download
     * quando ele ainda não está armazenado.
     */
    public File obter(String link) throws Exception {

        // Primeiro verifica se o GIF já existe.
        File arquivo = localizar(link);

        if (arquivo != null) {
            return arquivo;
        }

        // Se não existir, realiza o download.
        return baixar(link);
    }

    /**
     * Valida o arquivo retornado pelo downloader.
     */
    private void validarArquivo(File arquivo) {

        if (arquivo == null ||
                !arquivo.isFile() ||
                arquivo.length() == 0) {

            throw new IllegalStateException(
                    "Não foi possível obter um arquivo GIF válido."
            );
        }
    }

    /**
     * Valida e extrai o ID do arquivo.
     */
    private String obterId(String link) {

        if (link == null || link.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O link do Google Drive não pode ser vazio."
            );
        }

        String id = GoogleDriveUtils.extrairId(link);

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Link inválido do Google Drive."
            );
        }

        return id;
    }
}