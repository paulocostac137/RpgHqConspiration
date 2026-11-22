package com.example.rpghqconspiration;

/** Maps the control travel range to discrete frame positions. */
public final class GifFramePositionMapper {

    private static final float PROGRESSO_LIMITE_SOMBRA_INFERIOR = 0.9f;
    private static final float PROGRESSO_LIMITE_SOMBRA_SUPERIOR = 0.1f;

    private GifFramePositionMapper() {
    }

    public static int frameOffsetForProgress(int frameCount, float progress) {
        validarQuantidadeQuadros(frameCount);
        float limitado = limitarProgresso(progress);
        return Math.round(limitado * (frameCount - 1));
    }

    public static boolean entrouNaSombraInferior(float progress) {
        return limitarProgresso(progress) >=
                PROGRESSO_LIMITE_SOMBRA_INFERIOR;
    }

    public static boolean entrouNaSombraSuperior(float progress) {
        return limitarProgresso(progress) <=
                PROGRESSO_LIMITE_SOMBRA_SUPERIOR;
    }

    public static float progressoAposSombraSuperior(float progress) {
        return Math.max(
                0f,
                Math.min(
                        1f,
                        (limitarProgresso(progress) -
                                PROGRESSO_LIMITE_SOMBRA_SUPERIOR) /
                                (1f - PROGRESSO_LIMITE_SOMBRA_SUPERIOR)
                )
        );
    }

    public static boolean controlUsesOnOutRange(
            int frameOffset,
            int onFrameOffset,
            boolean movingUp) {
        if (onFrameOffset < 0 || frameOffset < 0) {
            throw new IndexOutOfBoundsException(
                    "A posição do quadro deve ser válida."
            );
        }
        return frameOffset > onFrameOffset ||
                (frameOffset == onFrameOffset && movingUp);
    }

    public static int frameOffsetForControlProgress(
            int frameCount,
            int onFrameOffset,
            float progress,
            boolean faixaOnOutAtiva) {
        validarQuantidadeQuadros(frameCount);
        if (onFrameOffset < 0 || onFrameOffset >= frameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro On1 precisa pertencer ao intervalo Go1–Out1."
            );
        }

        float limitado = limitarProgresso(progress);
        if (faixaOnOutAtiva) {
            return frameCount - 1 - Math.round(
                    limitado * (frameCount - 1 - onFrameOffset)
            );
        }
        return Math.round(limitado * onFrameOffset);
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
