package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GifFramePositionMapperTest {

    @Test
    public void distribuiTodosOsQuadrosEntreAsExtremidades() {
        int frameCount = 24;

        assertEquals(
                0,
                GifFramePositionMapper.frameOffsetForProgress(frameCount, 0f)
        );
        assertEquals(
                frameCount - 1,
                GifFramePositionMapper.frameOffsetForProgress(frameCount, 1f)
        );

        for (int i = 0; i < frameCount; i++) {
            float progress =
                    GifFramePositionMapper.progressForFrameOffset(
                            frameCount,
                            i
                    );
            assertEquals(
                    i,
                    GifFramePositionMapper.frameOffsetForProgress(
                            frameCount,
                            progress
                    )
            );
        }
    }

    @Test
    public void quadroUnicoPermaneceNaExtremidadeInicial() {
        assertEquals(
                0,
                GifFramePositionMapper.frameOffsetForProgress(1, 1f)
        );
        assertEquals(
                0f,
                GifFramePositionMapper.progressForFrameOffset(1, 0),
                0f
        );
    }

    @Test
    public void cadaFaixaUsaTodoOPercursoDoControle() {
        int frameCount = 11;
        int onFrameOffset = 5;

        assertEquals(
                0,
                GifFramePositionMapper.frameOffsetForControlProgress(
                        frameCount,
                        onFrameOffset,
                        0f,
                        false
                )
        );
        assertEquals(
                onFrameOffset,
                GifFramePositionMapper.frameOffsetForControlProgress(
                        frameCount,
                        onFrameOffset,
                        1f,
                        false
                )
        );
        assertEquals(
                frameCount - 1,
                GifFramePositionMapper.frameOffsetForControlProgress(
                        frameCount,
                        onFrameOffset,
                        0f,
                        true
                )
        );
        assertEquals(
                onFrameOffset,
                GifFramePositionMapper.frameOffsetForControlProgress(
                        frameCount,
                        onFrameOffset,
                        1f,
                        true
                )
        );
    }

    @Test
    public void sombraInferiorAtivaFaixaOnOut() {
        assertEquals(
                false,
                GifFramePositionMapper.entrouNaSombraInferior(0.89f)
        );
        assertEquals(
                true,
                GifFramePositionMapper.entrouNaSombraInferior(0.9f)
        );
        assertEquals(
                7,
                GifFramePositionMapper.frameOffsetForControlProgress(
                        11,
                        5,
                        0.5f,
                        true
                )
        );
    }

    @Test
    public void sombraSuperiorDetectaAEntradaPeloLimiteSuperior() {
        assertEquals(
                true,
                GifFramePositionMapper.entrouNaSombraSuperior(0.1f)
        );
        assertEquals(
                true,
                GifFramePositionMapper.entrouNaSombraSuperior(0.09f)
        );
        assertEquals(
                false,
                GifFramePositionMapper.entrouNaSombraSuperior(0.11f)
        );
        assertEquals(
                0f,
                GifFramePositionMapper.progressoAposSombraSuperior(0.1f),
                0f
        );
        assertEquals(
                1f,
                GifFramePositionMapper.progressoAposSombraSuperior(1f),
                0.0001f
        );
    }

    @Test
    public void selecionaSubtrechoPelaPosicaoAtualEConsideraDirecaoNoOn() {
        assertEquals(
                false,
                GifFramePositionMapper.controlUsesOnOutRange(3, 5, true)
        );
        assertEquals(
                true,
                GifFramePositionMapper.controlUsesOnOutRange(7, 5, false)
        );
        assertEquals(
                true,
                GifFramePositionMapper.controlUsesOnOutRange(5, 5, true)
        );
        assertEquals(
                false,
                GifFramePositionMapper.controlUsesOnOutRange(5, 5, false)
        );
    }

    @Test
    public void movimentoNosDoisSentidosPermaneceNoSubtrechoSelecionado() {
        int frameCount = 11;
        int onFrameOffset = 5;
        int anteriorGoOn = -1;
        int anteriorOnOut = -1;

        for (int passo = 0; passo <= 100; passo++) {
            float progresso = passo / 100f;
            int quadroGoOn =
                    GifFramePositionMapper.frameOffsetForControlProgress(
                            frameCount,
                            onFrameOffset,
                            progresso,
                            false
                    );
            int quadroOnOut =
                    GifFramePositionMapper.frameOffsetForControlProgress(
                            frameCount,
                            onFrameOffset,
                            1f - progresso,
                            true
                    );

            assertEquals(true, quadroGoOn >= 0 && quadroGoOn <= onFrameOffset);
            assertEquals(
                    true,
                    quadroOnOut >= onFrameOffset &&
                            quadroOnOut < frameCount
            );
            assertEquals(true, quadroGoOn >= anteriorGoOn);
            assertEquals(true, quadroOnOut >= anteriorOnOut);
            anteriorGoOn = quadroGoOn;
            anteriorOnOut = quadroOnOut;
        }
    }

}
