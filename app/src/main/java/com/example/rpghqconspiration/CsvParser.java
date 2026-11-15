package com.example.rpghqconspiration;

import java.util.ArrayList;

/**
 * Responsável por interpretar linhas no formato CSV.
 *
 * Identifica vírgulas que separam colunas, respeitando
 * vírgulas dentro de campos delimitados por aspas.
 */
public final class CsvParser {

    private CsvParser() {
        // Impede a criação de instâncias.
    }

    /**
     * Separa uma linha CSV em suas respectivas colunas.
     *
     * Também interpreta aspas duplas escapadas ("").
     */
    public static ArrayList<String> separar(String linha) {

        ArrayList<String> resultado =
                new ArrayList<>();

        StringBuilder campo =
                new StringBuilder();

        boolean dentroAspas = false;

        for (int i = 0; i < linha.length(); i++) {

            char caractere = linha.charAt(i);

            if (caractere == '"') {

                if (dentroAspas &&
                        i + 1 < linha.length() &&
                        linha.charAt(i + 1) == '"') {

                    campo.append('"');

                    i++;

                } else {

                    dentroAspas = !dentroAspas;
                }

            } else if (caractere == ',' &&
                    !dentroAspas) {

                resultado.add(campo.toString());

                campo.setLength(0);

            } else {

                campo.append(caractere);
            }
        }

        resultado.add(campo.toString());

        return resultado;
    }
}