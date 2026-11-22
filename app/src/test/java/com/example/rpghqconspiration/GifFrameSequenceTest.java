package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class GifFrameSequenceTest {

    @Test
    public void alternaAExibicaoDaFaixaPorSeisPassagens() {
        assertArrayEquals(
                new int[]{
                        0,
                        1, 2, 3,
                        3, 2, 1,
                        1, 2, 3,
                        3, 2, 1,
                        1, 2, 3,
                        3, 2, 1,
                        4
                },
                GifFrameSequence.expandFrameIndexes(
                        5,
                        new FaixaRepeticaoGif(2, 4),
                        6
                )
        );
    }

    @Test
    public void repeteSomenteARegiaoGo1AOut1AlternandoDirecaoCincoVezes() {
        assertArrayEquals(
                new int[]{
                        0,
                        1, 2, 3,
                        3, 2, 1,
                        1, 2, 3,
                        3, 2, 1,
                        1, 2, 3,
                        4
                },
                GifFrameSequence.expandFrameIndexes(
                        5,
                        new FaixaRepeticaoGif(2, 4),
                        5
                )
        );
    }

    @Test
    public void semFaixaMantemUmaReproducaoDeTodosOsQuadros() {
        assertArrayEquals(
                new int[]{0, 1, 2, 3},
                GifFrameSequence.expandFrameIndexes(4, null, 1)
        );
        assertArrayEquals(
                new int[]{4},
                GifFrameSequence.expandCycleFrameCounts(4, null, 1)
        );
        assertArrayEquals(
                new int[]{0, 4},
                GifFrameSequence.expandCycleStarts(4, null, 1)
        );
    }

    @Test
    public void separaPrefixoRepeticoesDaFaixaESufixoEmCiclos() {
        FaixaRepeticaoGif faixa = new FaixaRepeticaoGif(3, 5);

        assertArrayEquals(
                new int[]{2, 3, 3, 3, 3, 3, 3},
                GifFrameSequence.expandCycleFrameCounts(8, faixa, 5)
        );
        assertArrayEquals(
                new int[]{0, 2, 5, 8, 11, 14, 17, 20},
                GifFrameSequence.expandCycleStarts(8, faixa, 5)
        );
    }

    @Test
    public void localizaOCicloCorrespondenteAQuadrosAntesDuranteEDepoisDaFaixa() {
        int[] starts = GifFrameSequence.expandCycleStarts(
                8,
                new FaixaRepeticaoGif(3, 5),
                5
        );

        assertEquals(0, GifFrameSequence.cycleIndexForFrame(starts, 20, 0));
        assertEquals(0, GifFrameSequence.cycleIndexForFrame(starts, 20, 1));
        assertEquals(1, GifFrameSequence.cycleIndexForFrame(starts, 20, 2));
        assertEquals(2, GifFrameSequence.cycleIndexForFrame(starts, 20, 5));
        assertEquals(5, GifFrameSequence.cycleIndexForFrame(starts, 20, 14));
        assertEquals(6, GifFrameSequence.cycleIndexForFrame(starts, 20, 17));
        assertEquals(6, GifFrameSequence.cycleIndexForFrame(starts, 20, 19));
    }

    @Test
    public void registraOn1EmCadaRepeticaoDaFaixaGo1Out1() {
        assertArrayEquals(
                new int[]{-1, 1, 1, 1, 1, 1, -1},
                GifFrameSequence.expandCycleOnFrameOffsets(
                        8,
                        new FaixaRepeticaoGif(3, 4, 5),
                        5
                )
        );
    }

    @Test
    public void identificaTrechosAntesEDepoisDoOn() {
        int[] offsets = {2, 0, 5};
        int[] counts = {6, 4, 6};

        assertEquals(
                true,
                GifFrameSequence.hasRangeBeforeOn(offsets, 0)
        );
        assertEquals(
                true,
                GifFrameSequence.hasRangeAfterOn(offsets, counts, 0)
        );
        assertEquals(
                false,
                GifFrameSequence.hasRangeBeforeOn(offsets, 1)
        );
        assertEquals(
                true,
                GifFrameSequence.hasRangeAfterOn(offsets, counts, 1)
        );
        assertEquals(
                false,
                GifFrameSequence.hasRangeAfterOn(offsets, counts, 2)
        );
    }

    @Test
    public void alternaPosicaoOnNasSeisPassagens() {
        assertArrayEquals(
                new int[]{-1, 0, 2, 0, 2, 0, 2, -1},
                GifFrameSequence.expandCycleOnFrameOffsets(
                        8,
                        new FaixaRepeticaoGif(3, 3, 5),
                        6
                )
        );
    }

    @Test
    public void expandeCadaGrupoMarcadoSeteVezesEConservaTrechosSemMarcador() {
        FaixaRepeticaoGif faixas = FaixaRepeticaoGif.agrupar(
                java.util.Arrays.asList(
                        new FaixaRepeticaoGif(2, 4, 4),
                        new FaixaRepeticaoGif(4, 5, 6)
                )
        );

        assertArrayEquals(
                new int[]{
                        0,
                        1, 2, 3, 3, 2, 1, 1, 2, 3, 3, 2, 1,
                        1, 2, 3, 3, 2, 1, 1, 2, 3,
                        3, 4, 5, 5, 4, 3, 3, 4, 5, 5, 4, 3,
                        3, 4, 5, 5, 4, 3, 3, 4, 5,
                        6, 7
                },
                GifFrameSequence.expandFrameIndexes(8, faixas, 7)
        );
        assertArrayEquals(
                new int[]{
                        1,
                        3, 3, 3, 3, 3, 3, 3,
                        3, 3, 3, 3, 3, 3, 3,
                        2
                },
                GifFrameSequence.expandCycleFrameCounts(8, faixas, 7)
        );
        assertArrayEquals(
                new int[]{
                        -1,
                        2, 0, 2, 0, 2, 0, 2,
                        1, 1, 1, 1, 1, 1, 1,
                        -1
                },
                GifFrameSequence.expandCycleOnFrameOffsets(
                        8,
                        faixas,
                        7
                )
        );
    }

    @Test
    public void aplicaRampasNoPrimeiroEUltimoCicloDeCadaGrupo() {
        FaixaRepeticaoGif faixas = FaixaRepeticaoGif.agrupar(
                java.util.Arrays.asList(
                        new FaixaRepeticaoGif(1, 10, 40),
                        new FaixaRepeticaoGif(40, 50, 70)
                )
        );
        int[] limites = GifFrameSequence.expandSpeedRampBounds(
                70,
                faixas,
                7
        );
        int[] primeiroInicio = {limites[0], limites[4]};
        int[] primeiroFim = {limites[1], limites[5]};
        int[] ultimoInicio = {limites[2], limites[6]};
        int[] ultimoFim = {limites[3], limites[7]};

        assertArrayEquals(
                new int[]{0, 39, 240, 279, 280, 310, 466, 496},
                limites
        );
        assertEquals(
                1f,
                GifFrameSequence.calcularFatorVelocidade(
                        0,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0f
        );
        assertEquals(
                0.2f,
                GifFrameSequence.calcularFatorVelocidade(
                        29,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0.001f
        );
        assertEquals(
                0.2f,
                GifFrameSequence.calcularFatorVelocidade(
                        100,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0.001f
        );
        assertEquals(
                0.2f,
                GifFrameSequence.calcularFatorVelocidade(
                        240,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0.001f
        );
        assertEquals(
                1f,
                GifFrameSequence.calcularFatorVelocidade(
                        279,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0f
        );
        assertEquals(
                1f,
                GifFrameSequence.calcularFatorVelocidade(
                        280,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0f
        );
        assertEquals(
                0.2f,
                GifFrameSequence.calcularFatorVelocidade(
                        309,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0.001f
        );
        assertEquals(
                0.2f,
                GifFrameSequence.calcularFatorVelocidade(
                        466,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0.001f
        );
        assertEquals(
                1f,
                GifFrameSequence.calcularFatorVelocidade(
                        496,
                        primeiroInicio,
                        primeiroFim,
                        ultimoInicio,
                        ultimoFim
                ),
                0f
        );
    }
}
