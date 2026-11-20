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
}
