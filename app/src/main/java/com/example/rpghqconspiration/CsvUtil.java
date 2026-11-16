package com.example.rpghqconspiration;

import java.util.ArrayList;

public final class CsvUtil {

    private CsvUtil() {
    }

    public static ArrayList<String> separarCSV(String linha) {

        ArrayList<String> resultado = new ArrayList<>();

        StringBuilder campo = new StringBuilder();

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

            } else if (caractere == ',' && !dentroAspas) {

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
