package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class GifFrameSequenceTest {

    @Test
    public void repeteSomenteARegiaoGo1AOut2CincoVezes() {
        assertArrayEquals(
                new int[]{
                        0,
                        1, 2, 3,
                        1, 2, 3,
                        1, 2, 3,
                        1, 2, 3,
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
    public void encontraGrupoOn1AnteriorAoGrupoAtual() {
        int[] offsets = {-1, 1, 1, 1, -1};

        assertEquals(
                -1,
                GifFrameSequence.previousControlCycleIndex(offsets, 1)
        );
        assertEquals(
                1,
                GifFrameSequence.previousControlCycleIndex(offsets, 2)
        );
        assertEquals(
                3,
                GifFrameSequence.previousControlCycleIndex(offsets, 4)
        );
    }
}
