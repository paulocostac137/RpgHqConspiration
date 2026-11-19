package com.example.rpghqconspiration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import java.util.Map;
import java.util.TreeMap;

public final class MarcadoresBarraView extends View {

    private final Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF areaFundo = new RectF();
    private final Map<Integer, StringBuilder> marcadores = new TreeMap<>();
    private final int[] barraLocalizacao = new int[2];
    private final int[] viewLocalizacao = new int[2];
    private SeekBar barra;
    private int totalQuadros;
    private float densidade;

    public MarcadoresBarraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    private void inicializar() {
        densidade = getResources().getDisplayMetrics().density;
        fundo.setColor(0xE6263238);
        texto.setColor(0xFFFFFFFF);
        texto.setTextAlign(Paint.Align.CENTER);
        texto.setTextSize(12f * densidade);
        texto.setFakeBoldText(true);
        setClickable(false);
        setFocusable(false);
    }

    public void configurar(SeekBar barra, int totalQuadros) {
        this.barra = barra;
        this.totalQuadros = Math.max(0, totalQuadros);
        invalidate();
    }

    public void adicionarMarcador(int indiceQuadro, String letra) {
        if (totalQuadros <= 0 ||
                indiceQuadro < 0 || indiceQuadro >= totalQuadros ||
                letra == null || letra.isEmpty()) {
            return;
        }

        StringBuilder letras = marcadores.get(indiceQuadro);
        if (letras == null) {
            letras = new StringBuilder();
            marcadores.put(indiceQuadro, letras);
        }
        letras.append(letra);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (barra == null || totalQuadros <= 0 || marcadores.isEmpty()) {
            return;
        }

        barra.getLocationOnScreen(barraLocalizacao);
        getLocationOnScreen(viewLocalizacao);
        float esquerda = barraLocalizacao[0] - viewLocalizacao[0]
                + barra.getPaddingLeft();
        float larguraUtil = barra.getWidth()
                - barra.getPaddingLeft()
                - barra.getPaddingRight();
        float centroY = barraLocalizacao[1] - viewLocalizacao[1]
                + barra.getHeight() / 2f - 18f * densidade;
        float raio = 9f * densidade;
        float metadeAltura = 10f * densidade;

        for (Map.Entry<Integer, StringBuilder> marcador
                : marcadores.entrySet()) {
            float proporcao = totalQuadros == 1
                    ? 0f
                    : (float) marcador.getKey() / (totalQuadros - 1);
            String letras = marcador.getValue().toString();
            float meiaLargura = Math.max(
                    raio,
                    texto.measureText(letras) / 2f + 4f * densidade
            );
            float centroX = esquerda + larguraUtil * proporcao;
            centroX = Math.max(
                    meiaLargura,
                    Math.min(getWidth() - meiaLargura, centroX)
            );
            areaFundo.set(
                    centroX - meiaLargura,
                    centroY - metadeAltura,
                    centroX + meiaLargura,
                    centroY + metadeAltura
            );
            canvas.drawRoundRect(
                    areaFundo,
                    5f * densidade,
                    5f * densidade,
                    fundo
            );
            Paint.FontMetrics metricas = texto.getFontMetrics();
            float linhaBase = centroY -
                    (metricas.ascent + metricas.descent) / 2f;
            canvas.drawText(letras, centroX, linhaBase, texto);
        }
    }
}
