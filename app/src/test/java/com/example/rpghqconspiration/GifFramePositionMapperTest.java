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
}
