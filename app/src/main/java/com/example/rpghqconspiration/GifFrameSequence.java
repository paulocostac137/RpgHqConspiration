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
    private final int[] cycleFrameCounts;
    private final long durationMs;
    private boolean released;

    private GifFrameSequence(
            Bitmap[] frames,
            Bitmap[] ownedFrames,
            int[] segmentStarts,
            int[] cycleFrameCounts,
            long durationMs) {
        this.frames = frames;
        this.ownedFrames = ownedFrames;
        this.segmentStarts = segmentStarts;
        this.cycleFrameCounts = cycleFrameCounts;
        this.durationMs = durationMs;
    }

    public static GifFrameSequence extract(
            Movie movie,
            GifFrameInfo frameInfo,
            int maxDimension) {
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
        int frameCount = frameInfo.getFrameCountAt30Fps();
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
        int uniqueFrameCount = Math.min(sourceFrameCount, frameCount);
        Bitmap[] uniqueFrames = new Bitmap[uniqueFrameCount];

        try {
            for (int i = 0; i < uniqueFrameCount; i++) {
                int sourceFrameIndex = sourceFrameCount >= frameCount
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

            Bitmap[] bitmaps = new Bitmap[frameCount];
            for (int i = 0; i < frameCount; i++) {
                int uniqueIndex =
                        frameInfo.getSourceFrameFor30FpsFrame(i);
                if (sourceFrameCount >= frameCount) {
                    uniqueIndex = i;
                }
                bitmaps[i] = uniqueFrames[uniqueIndex];
            }
            return new GifFrameSequence(
                    bitmaps,
                    uniqueFrames,
                    new int[]{0, bitmaps.length},
                    new int[]{bitmaps.length},
                    durationMs
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
        int[] starts = new int[sequences.size() + 1];
        int[] cycleFrameCounts = new int[sequences.size()];
        int frameOffset = 0;
        int ownedOffset = 0;

        for (int i = 0; i < sequences.size(); i++) {
            GifFrameSequence sequence = sequences.get(i);
            starts[i] = frameOffset;
            cycleFrameCounts[i] = sequence.getSegmentFrameCount(0);
            for (int repetition = 0; repetition < repetitions[i];
                 repetition++) {
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

        GifFrameSequence timeline = new GifFrameSequence(
                timelineFrames,
                timelineOwnedFrames,
                starts,
                cycleFrameCounts,
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
        return cycleFrameCounts[segmentIndex];
    }

    public int getSegmentTotalFrameCount(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= getSegmentCount()) {
            throw new IndexOutOfBoundsException("Índice de GIF inválido.");
        }
        return segmentStarts[segmentIndex + 1] - segmentStarts[segmentIndex];
    }

    public int getCycleOffsetForFrame(int segmentIndex, int frameIndex) {
        int segmentStart = getSegmentStartFrame(segmentIndex);
        int segmentFrameCount = getSegmentTotalFrameCount(segmentIndex);
        int cycleFrameCount = getSegmentFrameCount(segmentIndex);
        if (frameIndex < segmentStart ||
                frameIndex >= segmentStart + segmentFrameCount) {
            throw new IndexOutOfBoundsException(
                    "O quadro não pertence ao GIF selecionado."
            );
        }
        return (frameIndex - segmentStart) % cycleFrameCount;
    }

    public long getSegmentCycleDurationMs(int segmentIndex) {
        int cycleFrameCount = getSegmentFrameCount(segmentIndex);
        int totalFrameCount = getSegmentTotalFrameCount(segmentIndex);
        return Math.max(
                1L,
                durationMs * cycleFrameCount / frames.length
        );
    }

    public int getFrameIndexForCyclePosition(
            int segmentIndex,
            int currentFrameIndex,
            int cycleOffset) {
        int segmentStart = getSegmentStartFrame(segmentIndex);
        int cycleFrameCount = getSegmentFrameCount(segmentIndex);
        int currentCycleOffset = getCycleOffsetForFrame(
                segmentIndex,
                currentFrameIndex
        );
        if (cycleOffset < 0 || cycleOffset >= cycleFrameCount) {
            throw new IndexOutOfBoundsException(
                    "Posição de quadro fora do ciclo."
            );
        }
        int cycleStart = currentFrameIndex - currentCycleOffset;
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
