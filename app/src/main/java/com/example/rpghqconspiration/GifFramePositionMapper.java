package com.example.rpghqconspiration;

/** Maps the control travel range to discrete frame positions. */
public final class GifFramePositionMapper {

    private GifFramePositionMapper() {
    }

    public static int frameOffsetForProgress(int frameCount, float progress) {
        validarQuantidadeQuadros(frameCount);
        float limitado = limitarProgresso(progress);
        return Math.round(limitado * (frameCount - 1));
    }

    public static float progressForFrameOffset(
            int frameCount,
            int frameOffset) {
        validarQuantidadeQuadros(frameCount);
        if (frameOffset < 0 || frameOffset >= frameCount) {
            throw new IndexOutOfBoundsException(
                    "Posição de quadro fora do intervalo."
            );
        }
        return frameCount == 1
                ? 0f
                : (float) frameOffset / (frameCount - 1);
    }

    private static float limitarProgresso(float progress) {
        if (Float.isNaN(progress) || Float.isInfinite(progress)) {
            throw new IllegalArgumentException(
                    "O progresso deve ser um número finito."
            );
        }
        return Math.max(0f, Math.min(1f, progress));
    }

    private static void validarQuantidadeQuadros(int frameCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de quadros deve ser positiva."
            );
        }
    }
}
