package com.example.rpghqconspiration;

/**
 * Funções utilitárias relacionadas ao Google Drive.
 *
 * Não realiza conexões e não manipula Views.
 */
public final class GoogleDriveUtils {

    private GoogleDriveUtils() {
        // Impede a criação de instâncias.
    }

    /**
     * Extrai o ID de um arquivo a partir de um link
     * do Google Drive.
     */
    public static String extrairId(String link) {

        if (link == null || link.trim().isEmpty()) {
            return null;
        }

        try {

            String resultado = null;

            if (link.contains("/file/d/")) {

                String parte =
                        link.split("/file/d/")[1];

                resultado = parte.split("/")[0];

            } else if (link.contains("id=")) {

                String parte =
                        link.split("id=")[1];

                resultado = parte.split("&")[0];
            }

            if (resultado == null) {
                return null;
            }

            resultado = resultado.trim();

            if (resultado.isEmpty() ||
                    !resultado.matches("[a-zA-Z0-9_-]+")) {

                return null;
            }

            return resultado;

        } catch (Exception e) {

            return null;
        }
    }
}