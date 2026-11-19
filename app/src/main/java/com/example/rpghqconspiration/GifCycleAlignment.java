package com.example.rpghqconspiration;

/** Smoothly adjusts natural playback to meet a moving control target. */
public final class GifCycleAlignment {

    private static final float CATCH_UP_PER_FRAME = 0.04f;
    private static final float RESPONSE_TIME_MS = 220f;

    private int cycleFrameCount;
    private long cycleDurationMs;
    private int targetFrame;
    private int previousTargetFrame;
    private long previousTargetTimeMs;
    private long previousSpeedUpdateMs;
    private float targetSpeedMultiplier;
    private float speedMultiplier = 1f;
    private boolean initialized;
    private boolean recovering;

    public void begin(
            int cycleFrameCount,
            long cycleDurationMs,
            int currentFrame,
            int targetFrame,
            long nowMs) {
        validate(cycleFrameCount, cycleDurationMs, currentFrame, targetFrame);
        this.cycleFrameCount = cycleFrameCount;
        this.cycleDurationMs = cycleDurationMs;
        this.targetFrame = targetFrame;
        previousTargetFrame = targetFrame;
        previousTargetTimeMs = nowMs;
        previousSpeedUpdateMs = nowMs;
        targetSpeedMultiplier = 0f;
        speedMultiplier = 1f;
        initialized = true;
        recovering = false;
    }

    public void updateTarget(int targetFrame, long nowMs) {
        if (!initialized) {
            throw new IllegalStateException(
                    "O alinhamento do ciclo ainda não foi iniciado."
            );
        }
        if (targetFrame < 0 || targetFrame >= cycleFrameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro alvo não pertence ao ciclo."
            );
        }

        long elapsedMs = nowMs - previousTargetTimeMs;
        if (elapsedMs > 0L) {
            int targetMovement = signedCycleDistance(
                    previousTargetFrame,
                    targetFrame,
                    cycleFrameCount
            );
            float frameDurationMs =
                    (float) cycleDurationMs / cycleFrameCount;
            targetSpeedMultiplier = clamp(
                    targetMovement * frameDurationMs / elapsedMs,
                    0f,
                    1f
            );
        }

        this.targetFrame = targetFrame;
        previousTargetFrame = targetFrame;
        previousTargetTimeMs = nowMs;
    }

    public float updateSpeed(int currentFrame, long nowMs) {
        if (!initialized) {
            throw new IllegalStateException(
                    "O alinhamento do ciclo ainda não foi iniciado."
            );
        }
        if (currentFrame < 0 || currentFrame >= cycleFrameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro atual não pertence ao ciclo."
            );
        }

        int framesUntilTarget = Math.floorMod(
                targetFrame - currentFrame,
                cycleFrameCount
        );
        float desiredSpeed = clamp(
                targetSpeedMultiplier +
                        framesUntilTarget * CATCH_UP_PER_FRAME,
                0f,
                1f
        );
        long elapsedMs = Math.max(0L, nowMs - previousSpeedUpdateMs);
        previousSpeedUpdateMs = nowMs;
        float response = (float) (
                1d - Math.exp(-elapsedMs / (double) RESPONSE_TIME_MS)
        );
        speedMultiplier +=
                (desiredSpeed - speedMultiplier) * response;
        speedMultiplier = clamp(speedMultiplier, 0f, 1f);
        return speedMultiplier;
    }

    public void beginRecovery(long nowMs) {
        recovering = speedMultiplier < 0.999f;
        previousSpeedUpdateMs = nowMs;
        if (!recovering) {
            speedMultiplier = 1f;
            initialized = false;
        }
    }

    public float updateRecovery(long nowMs) {
        if (!recovering) {
            return 1f;
        }
        long elapsedMs = Math.max(0L, nowMs - previousSpeedUpdateMs);
        previousSpeedUpdateMs = nowMs;
        float response = (float) (
                1d - Math.exp(-elapsedMs / (double) RESPONSE_TIME_MS)
        );
        speedMultiplier += (1f - speedMultiplier) * response;
        if (speedMultiplier >= 0.999f) {
            speedMultiplier = 1f;
            recovering = false;
            initialized = false;
        }
        return speedMultiplier;
    }

    public boolean isRecovering() {
        return recovering;
    }

    public int getTargetFrame() {
        if (!initialized) {
            throw new IllegalStateException(
                    "O alinhamento do ciclo ainda não foi iniciado."
            );
        }
        return targetFrame;
    }

    public void reset() {
        initialized = false;
        cycleFrameCount = 0;
        cycleDurationMs = 0L;
        targetFrame = 0;
        previousTargetFrame = 0;
        previousTargetTimeMs = 0L;
        previousSpeedUpdateMs = 0L;
        targetSpeedMultiplier = 0f;
        speedMultiplier = 1f;
        recovering = false;
    }

    private static int signedCycleDistance(
            int from,
            int to,
            int cycleFrameCount) {
        int forward = Math.floorMod(to - from, cycleFrameCount);
        return forward <= cycleFrameCount / 2
                ? forward
                : forward - cycleFrameCount;
    }

    private static void validate(
            int cycleFrameCount,
            long cycleDurationMs,
            int currentFrame,
            int targetFrame) {
        if (cycleFrameCount <= 0 ||
                cycleDurationMs <= 0L ||
                currentFrame < 0 ||
                currentFrame >= cycleFrameCount ||
                targetFrame < 0 ||
                targetFrame >= cycleFrameCount) {
            throw new IllegalArgumentException(
                    "Os dados de alinhamento do ciclo são inválidos."
            );
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
