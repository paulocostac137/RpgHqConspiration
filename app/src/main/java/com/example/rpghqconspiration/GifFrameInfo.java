package com.example.rpghqconspiration;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Reads frame boundaries and delays from a GIF stream. */
public final class GifFrameInfo {

    private static final int MIN_FRAME_DELAY_MS = 10;

    private final int[] frameDelaysMs;
    private final long[] frameStartsMs;
    private final long cycleDurationMs;

    private GifFrameInfo(int[] frameDelaysMs, long cycleDurationMs) {
        this.frameDelaysMs = frameDelaysMs;
        this.cycleDurationMs = cycleDurationMs;
        frameStartsMs = new long[frameDelaysMs.length];
        long startMs = 0L;
        for (int i = 0; i < frameDelaysMs.length; i++) {
            frameStartsMs[i] = startMs;
            startMs += frameDelaysMs[i];
        }
    }

    public static GifFrameInfo read(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() <= 0) {
            throw new IOException("Arquivo GIF inválido.");
        }

        try (InputStream input = new FileInputStream(file)) {
            return read(input);
        }
    }

    static GifFrameInfo read(InputStream input) throws IOException {
        byte[] header = readBytes(input, 6);
        String signature = new String(header, "US-ASCII");
        if (!"GIF87a".equals(signature) && !"GIF89a".equals(signature)) {
            throw new IOException("Assinatura GIF inválida.");
        }

        byte[] logicalScreenDescriptor = readBytes(input, 7);
        int screenPacked = logicalScreenDescriptor[4] & 0xFF;
        if ((screenPacked & 0x80) != 0) {
            skipFully(input, 3L * (1 << ((screenPacked & 0x07) + 1)));
        }

        int pendingDelayMs = 0;
        long cycleDurationMs = 0L;
        int frameCount = 0;
        int[] delays = new int[16];

        while (true) {
            int introducer = input.read();
            if (introducer < 0) {
                throw new EOFException("GIF sem terminador.");
            }
            if (introducer == 0x3B) {
                break;
            }

            if (introducer == 0x21) {
                int label = readUnsignedByte(input);
                if (label == 0xF9) {
                    int blockSize = readUnsignedByte(input);
                    byte[] control = readBytes(input, blockSize);
                    if (blockSize >= 4) {
                        int delayCentiseconds =
                                (control[1] & 0xFF)
                                        | ((control[2] & 0xFF) << 8);
                        pendingDelayMs = delayCentiseconds * 10;
                    }
                    int terminator = readUnsignedByte(input);
                    if (terminator != 0) {
                        throw new IOException(
                                "Bloco de controle GIF inválido."
                        );
                    }
                } else {
                    skipSubBlocks(input);
                }
                continue;
            }

            if (introducer != 0x2C) {
                throw new IOException("Estrutura de GIF inválida.");
            }

            byte[] imageDescriptor = readBytes(input, 9);
            int imagePacked = imageDescriptor[8] & 0xFF;
            if ((imagePacked & 0x80) != 0) {
                skipFully(input, 3L * (1 << ((imagePacked & 0x07) + 1)));
            }

            readUnsignedByte(input);
            skipSubBlocks(input);

            if (frameCount == delays.length) {
                int[] expanded = new int[delays.length * 2];
                System.arraycopy(delays, 0, expanded, 0, delays.length);
                delays = expanded;
            }

            int effectiveDelay = Math.max(
                    MIN_FRAME_DELAY_MS,
                    pendingDelayMs
            );
            delays[frameCount++] = effectiveDelay;
            cycleDurationMs += effectiveDelay;
            pendingDelayMs = 0;
        }

        if (frameCount == 0) {
            throw new IOException("O GIF não contém quadros.");
        }

        int[] frameDelays = new int[frameCount];
        System.arraycopy(delays, 0, frameDelays, 0, frameCount);
        return new GifFrameInfo(frameDelays, cycleDurationMs);
    }

    public int getFrameCount() {
        return frameDelaysMs.length;
    }

    public long getCycleDurationMs() {
        return cycleDurationMs;
    }

    public int getFrameCountAt30Fps() {
        long count = Math.max(
                1L,
                Math.round(cycleDurationMs * 30d / 1000d)
        );
        if (count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "O GIF excede o limite de quadros suportado."
            );
        }
        return (int) count;
    }

    public int getFrameCountAt30Fps(int cycleRepetitions) {
        if (cycleRepetitions <= 0) {
            throw new IllegalArgumentException(
                    "O número de repetições deve ser positivo."
            );
        }
        long count = (long) getFrameCountAt30Fps() * cycleRepetitions;
        if (count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "O GIF excede o limite de quadros suportado."
            );
        }
        return (int) count;
    }

    public int getFrameCountAt30Fps(
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        if (faixa == null || repeticoes <= 0) {
            throw new IllegalArgumentException(
                    "A faixa e o número de repetições devem ser válidos."
            );
        }
        int frameCount = getFrameCountAt30Fps();
        int primeiro = faixa.getPrimeiroQuadro();
        int quadroOn = faixa.getQuadroOn();
        int ultimo = faixa.getUltimoQuadro();
        if (primeiro > frameCount ||
                quadroOn > frameCount ||
                ultimo > frameCount) {
            throw new IllegalArgumentException(
                    "A faixa Go1–On1–Out1 excede a quantidade de quadros do GIF."
            );
        }

        long loopFrameCount = (long) ultimo - primeiro + 1L;
        long count = frameCount + loopFrameCount * (repeticoes - 1L);
        if (count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "O GIF excede o limite de quadros suportado."
            );
        }
        return (int) count;
    }

    public int getSourceFrameFor30FpsFrame(int presentationFrameIndex) {
        int presentationFrameCount = getFrameCountAt30Fps();
        if (presentationFrameIndex < 0 ||
                presentationFrameIndex >= presentationFrameCount) {
            throw new IndexOutOfBoundsException(
                    "Índice de quadro de apresentação inválido."
            );
        }
        return (int) (
                (long) presentationFrameIndex * frameDelaysMs.length
                        / presentationFrameCount
        );
    }

    public int getFrameTimeMs(int index, long effectiveCycleDurationMs) {
        if (index < 0 || index >= frameDelaysMs.length) {
            throw new IndexOutOfBoundsException("Índice de quadro inválido.");
        }

        long normalized = frameStartsMs[index] * effectiveCycleDurationMs
                / cycleDurationMs;
        return (int) Math.min(
                Math.max(0L, effectiveCycleDurationMs - 1L),
                normalized
        );
    }

    private static int readUnsignedByte(InputStream input)
            throws IOException {
        int value = input.read();
        if (value < 0) {
            throw new EOFException("GIF truncado.");
        }
        return value;
    }

    private static byte[] readBytes(InputStream input, int length)
            throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read < 0) {
                throw new EOFException("GIF truncado.");
            }
            offset += read;
        }
        return bytes;
    }

    private static void skipFully(InputStream input, long length)
            throws IOException {
        long remaining = length;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped <= 0) {
                readUnsignedByte(input);
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static void skipSubBlocks(InputStream input) throws IOException {
        while (true) {
            int length = readUnsignedByte(input);
            if (length == 0) {
                return;
            }
            skipFully(input, length);
        }
    }
}
