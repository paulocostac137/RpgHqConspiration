package com.example.rpghqconspiration;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;

import java.util.Arrays;
import java.util.List;

/** One GIF cycle sampled and normalized to 30 FPS. */
public final class GifFrameSequence {

    private final Bitmap[] frames;
    private final Bitmap[] ownedFrames;
    private final int[] segmentStarts;
    private final int[] cycleStarts;
    private final int[] cycleFrameCounts;
    private final int[] cycleOnFrameOffsets;
    private final long durationMs;
    private boolean released;

    private GifFrameSequence(
            Bitmap[] frames,
            Bitmap[] ownedFrames,
            int[] segmentStarts,
            int[] cycleStarts,
            int[] cycleFrameCounts,
            int[] cycleOnFrameOffsets,
            long durationMs) {
        this.frames = frames;
        this.ownedFrames = ownedFrames;
        this.segmentStarts = segmentStarts;
        this.cycleStarts = cycleStarts;
        this.cycleFrameCounts = cycleFrameCounts;
        this.cycleOnFrameOffsets = cycleOnFrameOffsets;
        this.durationMs = durationMs;
    }

    public static GifFrameSequence extract(
            Movie movie,
            GifFrameInfo frameInfo,
            int maxDimension) {
        return extract(movie, frameInfo, maxDimension, null, 1);
    }

    public static GifFrameSequence extract(
            Movie movie,
            GifFrameInfo frameInfo,
            int maxDimension,
            FaixaRepeticaoGif faixaRepetida,
            int repeticoesFaixa) {
        if (movie == null || movie.width() <= 0 || movie.height() <= 0) {
            throw new IllegalArgumentException(
                    "O GIF não contém dimensões válidas."
            );
        }
        if (frameInfo == null || maxDimension <= 0) {
            throw new IllegalArgumentException(
                    "Os metadados e dimensões devem ser válidos."
            );
        }

        long durationMs = frameInfo.getCycleDurationMs();
        int frameCount = faixaRepetida == null
                ? frameInfo.getFrameCountAt30Fps()
                : frameInfo.getFrameCountAt30Fps(
                        faixaRepetida,
                        repeticoesFaixa
                );
        int originalFrameCount = frameInfo.getFrameCountAt30Fps();
        int sourceFrameCount = frameInfo.getFrameCount();
        float scale = Math.min(
                1f,
                (float) maxDimension /
                        Math.max(movie.width(), movie.height())
        );
        int width = Math.max(1, Math.round(movie.width() * scale));
        int height = Math.max(1, Math.round(movie.height() * scale));
        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG
        );
        int uniqueFrameCount = Math.min(sourceFrameCount, originalFrameCount);
        Bitmap[] uniqueFrames = new Bitmap[uniqueFrameCount];

        try {
            for (int i = 0; i < uniqueFrameCount; i++) {
                int sourceFrameIndex = sourceFrameCount >= originalFrameCount
                        ? frameInfo.getSourceFrameFor30FpsFrame(i)
                        : i;
                Bitmap frame = Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.RGB_565
                );
                uniqueFrames[i] = frame;
                Canvas canvas = new Canvas(frame);
                canvas.drawColor(Color.BLACK);
                movie.setTime(
                        frameInfo.getFrameTimeMs(
                                sourceFrameIndex,
                                durationMs
                        )
                );
                canvas.scale(scale, scale);
                movie.draw(canvas, 0, 0, paint);
            }

            Bitmap[] originalFrames = new Bitmap[originalFrameCount];
            for (int i = 0; i < originalFrameCount; i++) {
                int uniqueIndex =
                        frameInfo.getSourceFrameFor30FpsFrame(i);
                if (sourceFrameCount >= originalFrameCount) {
                    uniqueIndex = i;
                }
                originalFrames[i] = uniqueFrames[uniqueIndex];
            }

            int[] frameIndexes = expandFrameIndexes(
                    originalFrameCount,
                    faixaRepetida,
                    repeticoesFaixa
            );
            Bitmap[] bitmaps = new Bitmap[frameCount];
            long expandedDurationMs = durationMs;
            for (int i = 0; i < frameIndexes.length; i++) {
                bitmaps[i] = originalFrames[frameIndexes[i]];
            }
            if (faixaRepetida != null) {
                int loopFrameCount = faixaRepetida.getUltimoQuadro()
                        - faixaRepetida.getPrimeiroQuadro() + 1;
                long loopDurationMs = Math.max(
                        1L,
                        Math.round(
                                durationMs * (double) loopFrameCount
                                        / originalFrameCount
                        )
                );
                expandedDurationMs +=
                        loopDurationMs * (repeticoesFaixa - 1L);
            }
            return new GifFrameSequence(
                    bitmaps,
                    uniqueFrames,
                    new int[]{0, bitmaps.length},
                    expandCycleStarts(
                            originalFrameCount,
                            faixaRepetida,
                            repeticoesFaixa
                    ),
                    expandCycleFrameCounts(
                            originalFrameCount,
                            faixaRepetida,
                            repeticoesFaixa
                    ),
                    expandCycleOnFrameOffsets(
                            originalFrameCount,
                            faixaRepetida,
                            repeticoesFaixa
                    ),
                    expandedDurationMs
            );
        } catch (RuntimeException erro) {
            for (Bitmap frame : uniqueFrames) {
                if (frame != null && !frame.isRecycled()) {
                    frame.recycle();
                }
            }
            throw erro;
        }
    }

    static int[] expandFrameIndexes(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        if (frameCount <= 0 || repeticoes <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de quadros e repetições deve ser positiva."
            );
        }
        if (faixa == null) {
            int[] indices = new int[frameCount];
            for (int i = 0; i < frameCount; i++) {
                indices[i] = i;
            }
            return indices;
        }
        int inicio = faixa.getPrimeiroQuadro() - 1;
        int fimExclusivo = faixa.getUltimoQuadro();
        if (fimExclusivo > frameCount) {
            throw new IllegalArgumentException(
                    "A faixa Go1-Out1 excede a quantidade de quadros do GIF."
            );
        }
        int comprimentoFaixa = fimExclusivo - inicio;
        long quantidadeQuadros =
                frameCount + (long) comprimentoFaixa * (repeticoes - 1L);
        if (quantidadeQuadros > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "A sequência de quadros excede o limite suportado."
            );
        }

        int[] indices = new int[(int) quantidadeQuadros];
        int destino = 0;
        for (int i = 0; i < inicio; i++) {
            indices[destino++] = i;
        }
        for (int repeticao = 0; repeticao < repeticoes; repeticao++) {
            for (int i = inicio; i < fimExclusivo; i++) {
                indices[destino++] = i;
            }
        }
        for (int i = fimExclusivo; i < frameCount; i++) {
            indices[destino++] = i;
        }
        return indices;
    }

    static int[] expandCycleStarts(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        int[] cycleCounts = expandCycleFrameCounts(
                frameCount,
                faixa,
                repeticoes
        );
        int[] starts = new int[cycleCounts.length + 1];
        for (int i = 0; i < cycleCounts.length; i++) {
            starts[i + 1] = starts[i] + cycleCounts[i];
        }
        return starts;
    }

    static int[] expandCycleFrameCounts(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        if (frameCount <= 0 || repeticoes <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade de quadros e repetições deve ser positiva."
            );
        }
        if (faixa == null) {
            return new int[]{frameCount};
        }

        int inicio = faixa.getPrimeiroQuadro() - 1;
        int fimExclusivo = faixa.getUltimoQuadro();
        if (fimExclusivo > frameCount) {
            throw new IllegalArgumentException(
                    "A faixa Go1-Out1 excede a quantidade de quadros do GIF."
            );
        }

        int quantidadeCiclos = repeticoes;
        if (inicio > 0) {
            quantidadeCiclos++;
        }
        if (fimExclusivo < frameCount) {
            quantidadeCiclos++;
        }
        int[] counts = new int[quantidadeCiclos];
        int ciclo = 0;
        if (inicio > 0) {
            counts[ciclo++] = inicio;
        }
        for (int i = 0; i < repeticoes; i++) {
            counts[ciclo++] = fimExclusivo - inicio;
        }
        if (fimExclusivo < frameCount) {
            counts[ciclo] = frameCount - fimExclusivo;
        }
        return counts;
    }

    static int[] expandCycleOnFrameOffsets(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        int[] cycleCounts = expandCycleFrameCounts(
                frameCount,
                faixa,
                repeticoes
        );
        int[] offsets = new int[cycleCounts.length];
        Arrays.fill(offsets, -1);
        if (faixa == null) {
            return offsets;
        }

        int indiceCiclo = faixa.getPrimeiroQuadro() > 1 ? 1 : 0;
        int offsetOn = faixa.getQuadroOn() - faixa.getPrimeiroQuadro();
        int ciclosRepetidos = repeticoes;
        for (int repeticao = 0; repeticao < ciclosRepetidos; repeticao++) {
            offsets[indiceCiclo + repeticao] = offsetOn;
        }
        return offsets;
    }

    public static GifFrameSequence concatenate(
            List<GifFrameSequence> sequences) {
        if (sequences == null) {
            throw new IllegalArgumentException(
                    "A linha do tempo precisa conter GIFs."
            );
        }
        int[] repetitions = new int[sequences.size()];
        Arrays.fill(repetitions, 1);
        return concatenate(sequences, repetitions);
    }

    public static GifFrameSequence concatenate(
            List<GifFrameSequence> sequences,
            int[] repetitions) {
        if (sequences == null || sequences.isEmpty()) {
            throw new IllegalArgumentException(
                    "A linha do tempo precisa conter GIFs."
            );
        }
        if (repetitions == null || repetitions.length != sequences.size()) {
            throw new IllegalArgumentException(
                    "Cada GIF precisa ter um número de repetições."
            );
        }

        int totalFrames = 0;
        int totalOwnedFrames = 0;
        long totalDurationMs = 0L;
        for (int i = 0; i < sequences.size(); i++) {
            GifFrameSequence sequence = sequences.get(i);
            if (sequence == null || sequence.released) {
                throw new IllegalArgumentException(
                        "A linha do tempo contém uma sequência inválida."
                );
            }
            int repeatCount = repetitions[i];
            if (repeatCount <= 0) {
                throw new IllegalArgumentException(
                        "O número de repetições deve ser positivo."
                );
            }
            long repeatedFrames = (long) sequence.frames.length * repeatCount;
            if (repeatedFrames > Integer.MAX_VALUE - totalFrames) {
                throw new IllegalArgumentException(
                        "A linha do tempo excede o limite de quadros."
                );
            }
            totalFrames += (int) repeatedFrames;
            totalOwnedFrames += sequence.ownedFrames.length;
            totalDurationMs += sequence.durationMs * repeatCount;
        }

        Bitmap[] timelineFrames = new Bitmap[totalFrames];
        Bitmap[] timelineOwnedFrames = new Bitmap[totalOwnedFrames];
        long totalCycles = 0L;
        for (int i = 0; i < sequences.size(); i++) {
            totalCycles += (long) sequences.get(i).cycleFrameCounts.length
                    * repetitions[i];
        }
        if (totalCycles > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "A linha do tempo excede o limite de ciclos."
            );
        }
        int cycleCount = (int) totalCycles;
        int[] starts = new int[sequences.size() + 1];
        int[] combinedCycleStarts = new int[cycleCount + 1];
        int[] cycleFrameCounts = new int[cycleCount];
        int[] cycleOnFrameOffsets = new int[cycleCount];
        int frameOffset = 0;
        int ownedOffset = 0;
        int cycleOffset = 0;

        for (int i = 0; i < sequences.size(); i++) {
            GifFrameSequence sequence = sequences.get(i);
            starts[i] = frameOffset;
            for (int repetition = 0; repetition < repetitions[i];
                 repetition++) {
                for (int cycle = 0;
                     cycle < sequence.cycleFrameCounts.length;
                     cycle++) {
                    int frameCount = sequence.cycleFrameCounts[cycle];
                    cycleFrameCounts[cycleOffset] = frameCount;
                    cycleOnFrameOffsets[cycleOffset] =
                            sequence.cycleOnFrameOffsets[cycle];
                    combinedCycleStarts[cycleOffset] =
                            frameOffset + sequence.cycleStarts[cycle];
                    cycleOffset++;
                }
                System.arraycopy(
                        sequence.frames,
                        0,
                        timelineFrames,
                        frameOffset,
                        sequence.frames.length
                );
                frameOffset += sequence.frames.length;
            }
            System.arraycopy(
                    sequence.ownedFrames,
                    0,
                    timelineOwnedFrames,
                    ownedOffset,
                    sequence.ownedFrames.length
            );
            ownedOffset += sequence.ownedFrames.length;
        }
        starts[sequences.size()] = frameOffset;
        combinedCycleStarts[cycleCount] = frameOffset;

        GifFrameSequence timeline = new GifFrameSequence(
                timelineFrames,
                timelineOwnedFrames,
                starts,
                combinedCycleStarts,
                cycleFrameCounts,
                cycleOnFrameOffsets,
                totalDurationMs
        );
        for (GifFrameSequence sequence : sequences) {
            sequence.released = true;
        }
        return timeline;
    }

    public int getFrameCount() {
        return frames.length;
    }

    public Bitmap getFrame(int index) {
        if (index < 0 || index >= frames.length) {
            throw new IndexOutOfBoundsException("Índice de quadro inválido.");
        }
        return frames[index];
    }

    public int getFrameIndex(float progress) {
        float limited = Math.max(0f, Math.min(1f, progress));
        return Math.min(
                frames.length - 1,
                (int) (limited * frames.length)
        );
    }

    public int getSegmentCount() {
        return segmentStarts.length - 1;
    }

    public int getSegmentIndex(int frameIndex) {
        int index = Math.max(0, Math.min(frames.length - 1, frameIndex));
        int insertionPoint = Arrays.binarySearch(segmentStarts, index);
        if (insertionPoint >= 0) {
            return Math.min(insertionPoint, getSegmentCount() - 1);
        }
        return -insertionPoint - 2;
    }

    public int getCycleIndexForFrame(int frameIndex) {
        return cycleIndexForFrame(
                cycleStarts,
                frames.length,
                frameIndex
        );
    }

    static int cycleIndexForFrame(
            int[] starts,
            int frameCount,
            int frameIndex) {
        int index = Math.max(0, Math.min(frameCount - 1, frameIndex));
        int insertionPoint = Arrays.binarySearch(starts, index);
        if (insertionPoint >= 0) {
            return Math.min(insertionPoint, starts.length - 2);
        }
        return -insertionPoint - 2;
    }

    public int getCycleStartFrame(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleFrameCounts.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return cycleStarts[cycleIndex];
    }

    public int getCycleFrameCount(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleFrameCounts.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return cycleFrameCounts[cycleIndex];
    }

    public int getCycleFrameOffsetForControlProgress(
            int cycleIndex,
            float progress,
            boolean faixaOnOutAtiva) {
        if (cycleIndex < 0 || cycleIndex >= cycleFrameCounts.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        int onOffset = cycleOnFrameOffsets[cycleIndex];
        if (onOffset < 0) {
            return GifFramePositionMapper.frameOffsetForProgress(
                    cycleFrameCounts[cycleIndex],
                    progress
            );
        }
        return GifFramePositionMapper.frameOffsetForControlProgress(
                cycleFrameCounts[cycleIndex],
                onOffset,
                progress,
                faixaOnOutAtiva
        );
    }

    public int getControlCycleIndexForFrame(int frameIndex) {
        int currentCycle = getCycleIndexForFrame(frameIndex);
        if (cycleOnFrameOffsets[currentCycle] >= 0) {
            return currentCycle;
        }

        int segmentIndex = getSegmentIndex(frameIndex);
        for (int cycle = 0; cycle < cycleOnFrameOffsets.length; cycle++) {
            if (cycleOnFrameOffsets[cycle] >= 0 &&
                    getSegmentIndex(cycleStarts[cycle]) == segmentIndex) {
                return cycle;
            }
        }
        return currentCycle;
    }

    public boolean hasPreviousControlCycle(int cycleIndex) {
        return previousControlCycleIndex(
                cycleOnFrameOffsets,
                cycleIndex
        ) >= 0;
    }

    static int previousControlCycleIndex(
            int[] controlFrameOffsets,
            int cycleIndex) {
        if (controlFrameOffsets == null ||
                cycleIndex < 0 ||
                cycleIndex >= controlFrameOffsets.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        for (int cycle = cycleIndex - 1; cycle >= 0; cycle--) {
            if (controlFrameOffsets[cycle] >= 0) {
                return cycle;
            }
        }
        return -1;
    }

    public int getCycleTotalFrameCount(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleFrameCounts.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return cycleStarts[cycleIndex + 1] - cycleStarts[cycleIndex];
    }

    public int getSegmentStartFrame(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= getSegmentCount()) {
            throw new IndexOutOfBoundsException("Índice de GIF inválido.");
        }
        return segmentStarts[segmentIndex];
    }

    public int getSegmentFrameCount(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= getSegmentCount()) {
            throw new IndexOutOfBoundsException("Índice de GIF inválido.");
        }
        return getSegmentTotalFrameCount(segmentIndex);
    }

    public int getSegmentTotalFrameCount(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= getSegmentCount()) {
            throw new IndexOutOfBoundsException("Índice de GIF inválido.");
        }
        return segmentStarts[segmentIndex + 1] - segmentStarts[segmentIndex];
    }

    public int getCycleOffsetForFrame(int segmentIndex, int frameIndex) {
        int cycleStart = getCycleStartFrame(segmentIndex);
        int cycleTotalFrameCount = getCycleTotalFrameCount(segmentIndex);
        int cycleFrameCount = getCycleFrameCount(segmentIndex);
        if (frameIndex < cycleStart ||
                frameIndex >= cycleStart + cycleTotalFrameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro não pertence ao ciclo selecionado."
            );
        }
        return frameIndex - cycleStart;
    }

    public long getCycleDurationMs(int segmentIndex) {
        int cycleFrameCount = getCycleFrameCount(segmentIndex);
        return Math.max(
                1L,
                durationMs * cycleFrameCount / frames.length
        );
    }

    public int getFrameIndexForCyclePosition(
            int segmentIndex,
            int currentFrameIndex,
            int cycleOffset) {
        int cycleStart = getCycleStartFrame(segmentIndex);
        int cycleFrameCount = getCycleFrameCount(segmentIndex);
        int currentCycleOffset = getCycleOffsetForFrame(
                segmentIndex,
                currentFrameIndex
        );
        if (cycleOffset < 0 || cycleOffset >= cycleFrameCount) {
            throw new IndexOutOfBoundsException(
                    "Posição de quadro fora do ciclo."
            );
        }
        return cycleStart + cycleOffset;
    }

    public int getFrameIndexForSegmentProgress(
            int segmentIndex,
            float progress) {
        int segmentStart = getSegmentStartFrame(segmentIndex);
        int segmentFrameCount = getSegmentFrameCount(segmentIndex);
        return segmentStart + GifFramePositionMapper.frameOffsetForProgress(
                segmentFrameCount,
                progress
        );
    }

    public float getProgressForFrameInSegment(
            int segmentIndex,
            int frameIndex) {
        int segmentStart = getSegmentStartFrame(segmentIndex);
        int segmentFrameCount = getSegmentFrameCount(segmentIndex);
        int frameOffset = frameIndex - segmentStart;
        return GifFramePositionMapper.progressForFrameOffset(
                segmentFrameCount,
                frameOffset
        );
    }

    public int getFrameForSegment(int segmentIndex) {
        return getSegmentStartFrame(segmentIndex);
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void liberar() {
        if (released) {
            return;
        }
        released = true;
        for (Bitmap frame : ownedFrames) {
            if (!frame.isRecycled()) {
                frame.recycle();
            }
        }
    }
}
