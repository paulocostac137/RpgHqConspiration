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
import java.util.ArrayList;
import java.util.List;

public final class MarcadoresBarraView extends View {

    private final Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF areaFundo = new RectF();
    private final Map<Integer, StringBuilder> marcadores = new TreeMap<>();
    private final List<Integer> ordemMarcacoes = new ArrayList<>();
    private final SequenciaMarcadores sequenciaMarcadores =
            new SequenciaMarcadores();
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

    public boolean adicionarMarcador(int indiceQuadro, String letra) {
        if (totalQuadros <= 0 ||
                indiceQuadro < 0 || indiceQuadro >= totalQuadros ||
                letra == null || letra.length() != 1) {
            return false;
        }

        char marcacao = letra.charAt(0);
        if (!sequenciaMarcadores.adicionar(marcacao, indiceQuadro)) {
            return false;
        }

        StringBuilder letras = marcadores.get(indiceQuadro);
        if (letras == null) {
            letras = new StringBuilder(3);
            marcadores.put(indiceQuadro, letras);
        }
        letras.append(marcacao);
        ordemMarcacoes.add(indiceQuadro);
        invalidate();
        return true;
    }

    public boolean podeAdicionarMarcador(int indiceQuadro, String letra) {
        if (totalQuadros <= 0 ||
                indiceQuadro < 0 || indiceQuadro >= totalQuadros ||
                letra == null || letra.length() != 1) {
            return false;
        }

        return sequenciaMarcadores.podeAdicionar(
                letra.charAt(0),
                indiceQuadro
        );
    }

    public String obterRotuloBotaoOn() {
        return sequenciaMarcadores.obterRotuloOn();
    }

    public boolean temPeloMenosTresMarcacoes() {
        return sequenciaMarcadores.temPeloMenosTresMarcacoes();
    }

    public FaixaRepeticaoGif obterFaixasRepetidas() {
        if (ordemMarcacoes.isEmpty() || ordemMarcacoes.size() % 3 != 0) {
            throw new IllegalStateException(
                    "Complete os marcadores Go, On e Out para visualizar."
            );
        }

        List<FaixaRepeticaoGif> faixas = new ArrayList<>();
        for (int indice = 0; indice < ordemMarcacoes.size(); indice += 3) {
            faixas.add(
                    new FaixaRepeticaoGif(
                            ordemMarcacoes.get(indice) + 1,
                            ordemMarcacoes.get(indice + 1) + 1,
                            ordemMarcacoes.get(indice + 2) + 1
                    )
            );
        }
        return FaixaRepeticaoGif.agrupar(faixas);
    }

    public List<String[]> obterLinhasPlanilha(
            String linkGif,
            String textoImagem) {
        if (linkGif == null || linkGif.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O link do GIF não pode estar vazio."
            );
        }

        if (textoImagem == null) {
            textoImagem = "";
        }
        if (textoImagem.length() > 100) {
            throw new IllegalArgumentException(
                    "O texto deve ter no máximo 100 caracteres."
            );
        }

        String[] colunasMarcadores =
                MarcadoresPlanilha.colunasPorCiclo(ordemMarcacoes);

        List<String[]> linhas = new ArrayList<>();
        boolean haMarcadores = false;
        for (String coluna : colunasMarcadores) {
            if (!coluna.isEmpty()) {
                haMarcadores = true;
                break;
            }
        }
        if (!haMarcadores) {
            return linhas;
        }
        String[] linha = new String[11];
        linha[0] = linkGif.trim();
        linha[1] = textoImagem;
        System.arraycopy(
                colunasMarcadores,
                0,
                linha,
                2,
                colunasMarcadores.length
        );
        linhas.add(linha);
        return linhas;
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
