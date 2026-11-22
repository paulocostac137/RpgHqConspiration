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

    private static final int QUADROS_DESACELERACAO = 30;
    private static final float VELOCIDADE_FINAL = 0.20f;
    static final int REPETICOES_POR_FAIXA = 7;

    private static final class ExpansaoFaixas {
        private final int[] indicesQuadros;
        private final int[] quantidadeQuadrosPorCiclo;
        private final int[] deslocamentosOnPorCiclo;
        private final int primeiroQuadroMarcado;
        private final int ultimoQuadroMarcado;
        private final int[] inicioPrimeiroCicloVelocidade;
        private final int[] fimPrimeiroCicloVelocidade;
        private final int[] inicioUltimoCicloVelocidade;
        private final int[] fimUltimoCicloVelocidade;

        private ExpansaoFaixas(
                int[] indicesQuadros,
                int[] quantidadeQuadrosPorCiclo,
                int[] deslocamentosOnPorCiclo,
                int primeiroQuadroMarcado,
                int ultimoQuadroMarcado,
                int[] inicioPrimeiroCicloVelocidade,
                int[] fimPrimeiroCicloVelocidade,
                int[] inicioUltimoCicloVelocidade,
                int[] fimUltimoCicloVelocidade) {
            this.indicesQuadros = indicesQuadros;
            this.quantidadeQuadrosPorCiclo =
                    quantidadeQuadrosPorCiclo;
            this.deslocamentosOnPorCiclo =
                    deslocamentosOnPorCiclo;
            this.primeiroQuadroMarcado = primeiroQuadroMarcado;
            this.ultimoQuadroMarcado = ultimoQuadroMarcado;
            this.inicioPrimeiroCicloVelocidade =
                    inicioPrimeiroCicloVelocidade;
            this.fimPrimeiroCicloVelocidade =
                    fimPrimeiroCicloVelocidade;
            this.inicioUltimoCicloVelocidade =
                    inicioUltimoCicloVelocidade;
            this.fimUltimoCicloVelocidade = fimUltimoCicloVelocidade;
        }
    }

    private final Bitmap[] frames;
    private final Bitmap[] ownedFrames;
    private final int[] segmentStarts;
    private final int[] cycleStarts;
    private final int[] cycleFrameCounts;
    private final int[] cycleOnFrameOffsets;
    private final long durationMs;
    private final int indicePrimeiroGo1;
    private final int indiceUltimoOut1;
    private final int[] inicioPrimeiroCicloVelocidade;
    private final int[] fimPrimeiroCicloVelocidade;
    private final int[] inicioUltimoCicloVelocidade;
    private final int[] fimUltimoCicloVelocidade;
    private boolean released;

    private GifFrameSequence(
            Bitmap[] frames,
            Bitmap[] ownedFrames,
            int[] segmentStarts,
            int[] cycleStarts,
            int[] cycleFrameCounts,
            int[] cycleOnFrameOffsets,
            long durationMs) {
        this(
                frames,
                ownedFrames,
                segmentStarts,
                cycleStarts,
                cycleFrameCounts,
                cycleOnFrameOffsets,
                durationMs,
                -1,
                -1,
                new int[0],
                new int[0],
                new int[0],
                new int[0]
        );
    }

    private GifFrameSequence(
            Bitmap[] frames,
            Bitmap[] ownedFrames,
            int[] segmentStarts,
            int[] cycleStarts,
            int[] cycleFrameCounts,
            int[] cycleOnFrameOffsets,
            long durationMs,
            int indicePrimeiroGo1,
            int indiceUltimoOut1) {
        this(
                frames,
                ownedFrames,
                segmentStarts,
                cycleStarts,
                cycleFrameCounts,
                cycleOnFrameOffsets,
                durationMs,
                indicePrimeiroGo1,
                indiceUltimoOut1,
                new int[0],
                new int[0],
                new int[0],
                new int[0]
        );
    }

    private GifFrameSequence(
            Bitmap[] frames,
            Bitmap[] ownedFrames,
            int[] segmentStarts,
            int[] cycleStarts,
            int[] cycleFrameCounts,
            int[] cycleOnFrameOffsets,
            long durationMs,
            int indicePrimeiroGo1,
            int indiceUltimoOut1,
            int[] inicioPrimeiroCicloVelocidade,
            int[] fimPrimeiroCicloVelocidade,
            int[] inicioUltimoCicloVelocidade,
            int[] fimUltimoCicloVelocidade) {
        this.frames = frames;
        this.ownedFrames = ownedFrames;
        this.segmentStarts = segmentStarts;
        this.cycleStarts = cycleStarts;
        this.cycleFrameCounts = cycleFrameCounts;
        this.cycleOnFrameOffsets = cycleOnFrameOffsets;
        this.durationMs = durationMs;
        this.indicePrimeiroGo1 = indicePrimeiroGo1;
        this.indiceUltimoOut1 = indiceUltimoOut1;
        this.inicioPrimeiroCicloVelocidade =
                inicioPrimeiroCicloVelocidade;
        this.fimPrimeiroCicloVelocidade =
                fimPrimeiroCicloVelocidade;
        this.inicioUltimoCicloVelocidade =
                inicioUltimoCicloVelocidade;
        this.fimUltimoCicloVelocidade =
                fimUltimoCicloVelocidade;
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
        int originalFrameCount = frameInfo.getFrameCountAt30Fps();
        ExpansaoFaixas expansao = expandirFaixas(
                originalFrameCount,
                faixaRepetida,
                repeticoesFaixa
        );
        int frameCount = expansao.indicesQuadros.length;
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

            Bitmap[] bitmaps = new Bitmap[frameCount];
            for (int i = 0; i < expansao.indicesQuadros.length; i++) {
                bitmaps[i] = originalFrames[
                        expansao.indicesQuadros[i]
                ];
            }
            long expandedDurationMs = Math.max(
                    1L,
                    Math.round(
                            durationMs * (double) frameCount /
                                    originalFrameCount
                    )
            );
            return new GifFrameSequence(
                    bitmaps,
                    uniqueFrames,
                    new int[]{0, bitmaps.length},
                    expandCycleStarts(expansao.quantidadeQuadrosPorCiclo),
                    expansao.quantidadeQuadrosPorCiclo,
                    expansao.deslocamentosOnPorCiclo,
                    expandedDurationMs,
                    faixaRepetida == null
                            ? -1
                            : expansao.primeiroQuadroMarcado,
                    faixaRepetida == null
                            ? -1
                            : expansao.ultimoQuadroMarcado,
                    expansao.inicioPrimeiroCicloVelocidade,
                    expansao.fimPrimeiroCicloVelocidade,
                    expansao.inicioUltimoCicloVelocidade,
                    expansao.fimUltimoCicloVelocidade
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

    public GifFrameSequence criarPreview(FaixaRepeticaoGif faixaRepetida) {
        if (faixaRepetida == null) {
            throw new IllegalArgumentException(
                    "Os marcadores são obrigatórios para o preview."
            );
        }
        if (released || frames.length == 0) {
            throw new IllegalStateException(
                    "Não foi possível preparar a sequência de preview."
            );
        }

        ExpansaoFaixas expansao = expandirFaixas(
                frames.length,
                faixaRepetida,
                REPETICOES_POR_FAIXA
        );
        Bitmap[] quadrosPreview = new Bitmap[expansao.indicesQuadros.length];
        for (int indice = 0; indice < quadrosPreview.length; indice++) {
            quadrosPreview[indice] =
                    frames[expansao.indicesQuadros[indice]];
        }

        long duracaoPreviewMs = Math.max(
                1L,
                Math.round(
                        durationMs * (double) quadrosPreview.length /
                                frames.length
                )
        );
        return new GifFrameSequence(
                quadrosPreview,
                new Bitmap[0],
                new int[]{0, quadrosPreview.length},
                expandCycleStarts(expansao.quantidadeQuadrosPorCiclo),
                expansao.quantidadeQuadrosPorCiclo,
                expansao.deslocamentosOnPorCiclo,
                duracaoPreviewMs,
                expansao.primeiroQuadroMarcado,
                expansao.ultimoQuadroMarcado,
                expansao.inicioPrimeiroCicloVelocidade,
                expansao.fimPrimeiroCicloVelocidade,
                expansao.inicioUltimoCicloVelocidade,
                expansao.fimUltimoCicloVelocidade
        );
    }

    static int[] expandFrameIndexes(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        return expandirFaixas(frameCount, faixa, repeticoes).indicesQuadros;
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
        return expandirFaixas(
                frameCount,
                faixa,
                repeticoes
        ).quantidadeQuadrosPorCiclo;
    }

    static int[] expandCycleOnFrameOffsets(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        return expandirFaixas(
                frameCount,
                faixa,
                repeticoes
        ).deslocamentosOnPorCiclo;
    }

    static int[] expandSpeedRampBounds(
            int frameCount,
            FaixaRepeticaoGif faixa,
            int repeticoes) {
        ExpansaoFaixas expansion = expandirFaixas(
                frameCount,
                faixa,
                repeticoes
        );
        int[] bounds = new int[
                expansion.inicioPrimeiroCicloVelocidade.length * 4
        ];
        for (int group = 0;
             group < expansion.inicioPrimeiroCicloVelocidade.length;
             group++) {
            int offset = group * 4;
            bounds[offset] =
                    expansion.inicioPrimeiroCicloVelocidade[group];
            bounds[offset + 1] =
                    expansion.fimPrimeiroCicloVelocidade[group];
            bounds[offset + 2] =
                    expansion.inicioUltimoCicloVelocidade[group];
            bounds[offset + 3] =
                    expansion.fimUltimoCicloVelocidade[group];
        }
        return bounds;
    }

    private static ExpansaoFaixas expandirFaixas(
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
            return new ExpansaoFaixas(
                    indices,
                    new int[]{frameCount},
                    new int[]{-1},
                    -1,
                    -1,
                    new int[0],
                    new int[0],
                    new int[0],
                    new int[0]
            );
        }

        List<FaixaRepeticaoGif> faixas = faixa.getFaixas();
        long totalQuadros = 0L;
        long totalCiclos = 0L;
        int cursor = 0;
        for (FaixaRepeticaoGif atual : faixas) {
            int inicio = atual.getPrimeiroQuadro() - 1;
            int fimExclusivo = atual.getUltimoQuadro();
            if (fimExclusivo > frameCount ||
                    atual.getQuadroOn() > frameCount) {
                throw new IllegalArgumentException(
                        "A faixa Go–On–Out excede a quantidade de quadros do GIF."
                );
            }
            if (inicio > cursor) {
                totalQuadros += inicio - cursor;
                totalCiclos++;
            }
            totalQuadros += (long) (fimExclusivo - inicio) * repeticoes;
            totalCiclos += repeticoes;
            cursor = Math.max(cursor, fimExclusivo);
        }
        if (cursor < frameCount) {
            totalQuadros += frameCount - cursor;
            totalCiclos++;
        }
        if (totalQuadros > Integer.MAX_VALUE ||
                totalCiclos > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "A sequência de quadros excede o limite suportado."
            );
        }

        int[] indices = new int[(int) totalQuadros];
        int[] counts = new int[(int) totalCiclos];
        int[] offsets = new int[(int) totalCiclos];
        int[] inicioPrimeiroCicloVelocidade = new int[faixas.size()];
        int[] fimPrimeiroCicloVelocidade = new int[faixas.size()];
        int[] inicioUltimoCicloVelocidade = new int[faixas.size()];
        int[] fimUltimoCicloVelocidade = new int[faixas.size()];
        Arrays.fill(offsets, -1);
        int destino = 0;
        int ciclo = 0;
        cursor = 0;
        int primeiroMarcado = -1;
        int ultimoMarcado = -1;
        int indiceFaixa = 0;
        for (FaixaRepeticaoGif atual : faixas) {
            int inicio = atual.getPrimeiroQuadro() - 1;
            int fimExclusivo = atual.getUltimoQuadro();
            for (int quadro = cursor; quadro < inicio; quadro++) {
                indices[destino++] = quadro;
                counts[ciclo]++;
            }
            if (inicio > cursor) {
                ciclo++;
            }
            if (primeiroMarcado < 0) {
                primeiroMarcado = destino;
            }
            int onOffset = atual.getQuadroOn() - 1 - inicio;
            int comprimento = fimExclusivo - inicio;
            for (int repeticao = 0; repeticao < repeticoes; repeticao++) {
                if (repeticao == 0) {
                    inicioPrimeiroCicloVelocidade[indiceFaixa] = destino;
                }
                if (repeticao == repeticoes - 1) {
                    inicioUltimoCicloVelocidade[indiceFaixa] = destino;
                }
                counts[ciclo] = comprimento;
                offsets[ciclo] = repeticao % 2 == 0
                        ? onOffset
                        : comprimento - 1 - onOffset;
                if (repeticao % 2 == 0) {
                    for (int quadro = inicio;
                         quadro < fimExclusivo;
                         quadro++) {
                        indices[destino++] = quadro;
                    }
                } else {
                    for (int quadro = fimExclusivo - 1;
                         quadro >= inicio;
                         quadro--) {
                        indices[destino++] = quadro;
                    }
                }
                if (repeticao == 0) {
                    fimPrimeiroCicloVelocidade[indiceFaixa] =
                            destino - 1;
                }
                ciclo++;
            }
            fimUltimoCicloVelocidade[indiceFaixa] = destino - 1;
            indiceFaixa++;
            ultimoMarcado = destino - 1;
            cursor = Math.max(cursor, fimExclusivo);
        }
        for (int quadro = cursor; quadro < frameCount; quadro++) {
            indices[destino++] = quadro;
            counts[ciclo]++;
        }
        return new ExpansaoFaixas(
                indices,
                counts,
                offsets,
                primeiroMarcado,
                ultimoMarcado,
                inicioPrimeiroCicloVelocidade,
                fimPrimeiroCicloVelocidade,
                inicioUltimoCicloVelocidade,
                fimUltimoCicloVelocidade
        );
    }

    private static int[] expandCycleStarts(int[] cycleCounts) {
        int[] starts = new int[cycleCounts.length + 1];
        for (int i = 0; i < cycleCounts.length; i++) {
            starts[i + 1] = starts[i] + cycleCounts[i];
        }
        return starts;
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
        long totalSpeedGroups = 0L;
        for (int i = 0; i < sequences.size(); i++) {
            totalCycles += (long) sequences.get(i).cycleFrameCounts.length
                    * repetitions[i];
            totalSpeedGroups += (long)
                    sequences.get(i).inicioPrimeiroCicloVelocidade.length *
                    repetitions[i];
        }
        if (totalCycles > Integer.MAX_VALUE ||
                totalSpeedGroups > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "A linha do tempo excede o limite de ciclos."
            );
        }
        int cycleCount = (int) totalCycles;
        int[] starts = new int[sequences.size() + 1];
        int[] combinedCycleStarts = new int[cycleCount + 1];
        int[] cycleFrameCounts = new int[cycleCount];
        int[] cycleOnFrameOffsets = new int[cycleCount];
        int[] combinedFirstCycleStarts = new int[(int) totalSpeedGroups];
        int[] combinedFirstCycleEnds = new int[(int) totalSpeedGroups];
        int[] combinedLastCycleStarts = new int[(int) totalSpeedGroups];
        int[] combinedLastCycleEnds = new int[(int) totalSpeedGroups];
        int frameOffset = 0;
        int ownedOffset = 0;
        int cycleOffset = 0;
        int speedGroupOffset = 0;
        int indicePrimeiroGo1Timeline = -1;
        int indiceUltimoOut1Timeline = -1;

        for (int i = 0; i < sequences.size(); i++) {
            GifFrameSequence sequence = sequences.get(i);
            if (indicePrimeiroGo1Timeline < 0 &&
                    sequence.indicePrimeiroGo1 >= 0) {
                indicePrimeiroGo1Timeline =
                        frameOffset + sequence.indicePrimeiroGo1;
            }
            starts[i] = frameOffset;
            for (int repetition = 0; repetition < repetitions[i];
                 repetition++) {
                if (sequence.indiceUltimoOut1 >= 0) {
                    indiceUltimoOut1Timeline =
                            frameOffset + sequence.indiceUltimoOut1;
                }
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
                for (int group = 0;
                     group < sequence.inicioPrimeiroCicloVelocidade.length;
                     group++) {
                    combinedFirstCycleStarts[speedGroupOffset] =
                            frameOffset +
                                    sequence.inicioPrimeiroCicloVelocidade[group];
                    combinedFirstCycleEnds[speedGroupOffset] =
                            frameOffset +
                                    sequence.fimPrimeiroCicloVelocidade[group];
                    combinedLastCycleStarts[speedGroupOffset] =
                            frameOffset +
                                    sequence.inicioUltimoCicloVelocidade[group];
                    combinedLastCycleEnds[speedGroupOffset] =
                            frameOffset +
                                    sequence.fimUltimoCicloVelocidade[group];
                    speedGroupOffset++;
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
                totalDurationMs,
                indicePrimeiroGo1Timeline,
                indiceUltimoOut1Timeline,
                combinedFirstCycleStarts,
                combinedFirstCycleEnds,
                combinedLastCycleStarts,
                combinedLastCycleEnds
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

    public int getCycleCount() {
        return cycleFrameCounts.length;
    }

    public int getCycleOnFrameOffset(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleOnFrameOffsets.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return cycleOnFrameOffsets[cycleIndex];
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

    public boolean hasRangeBeforeOn(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleOnFrameOffsets.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return hasRangeBeforeOn(
                cycleOnFrameOffsets,
                cycleIndex
        );
    }

    public boolean hasRangeAfterOn(int cycleIndex) {
        if (cycleIndex < 0 || cycleIndex >= cycleOnFrameOffsets.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return hasRangeAfterOn(
                cycleOnFrameOffsets,
                cycleFrameCounts,
                cycleIndex
        );
    }

    static boolean hasRangeBeforeOn(
            int[] controlFrameOffsets,
            int cycleIndex) {
        if (controlFrameOffsets == null ||
                cycleIndex < 0 ||
                cycleIndex >= controlFrameOffsets.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        return controlFrameOffsets[cycleIndex] > 0;
    }

    static boolean hasRangeAfterOn(
            int[] controlFrameOffsets,
            int[] cycleFrameCounts,
            int cycleIndex) {
        if (controlFrameOffsets == null ||
                cycleFrameCounts == null ||
                cycleIndex < 0 ||
                cycleIndex >= controlFrameOffsets.length ||
                cycleIndex >= cycleFrameCounts.length) {
            throw new IndexOutOfBoundsException("Índice de ciclo inválido.");
        }
        int onOffset = controlFrameOffsets[cycleIndex];
        return onOffset >= 0 &&
                onOffset < cycleFrameCounts[cycleIndex] - 1;
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

    public int getIndicePrimeiroGo1() {
        return indicePrimeiroGo1;
    }

    public int getIndiceUltimoOut1() {
        return indiceUltimoOut1;
    }

    public boolean isFrameProvenienteDeMarcadores(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= frames.length) {
            throw new IndexOutOfBoundsException("Índice de quadro inválido.");
        }
        int ciclo = cycleIndexForFrame(
                cycleStarts,
                frames.length,
                frameIndex
        );
        return cycleOnFrameOffsets[ciclo] >= 0;
    }

    public float getFatorVelocidade(int indiceQuadro) {
        return calcularFatorVelocidade(
                indiceQuadro,
                inicioPrimeiroCicloVelocidade,
                fimPrimeiroCicloVelocidade,
                inicioUltimoCicloVelocidade,
                fimUltimoCicloVelocidade
        );
    }

    static float calcularFatorVelocidade(
            int indiceQuadro,
            int[] inicioPrimeiroCiclo,
            int[] fimPrimeiroCiclo,
            int[] inicioUltimoCiclo,
            int[] fimUltimoCiclo) {
        if (inicioPrimeiroCiclo == null ||
                fimPrimeiroCiclo == null ||
                inicioUltimoCiclo == null ||
                fimUltimoCiclo == null ||
                inicioPrimeiroCiclo.length != fimPrimeiroCiclo.length ||
                inicioPrimeiroCiclo.length != inicioUltimoCiclo.length ||
                inicioPrimeiroCiclo.length != fimUltimoCiclo.length) {
            throw new IllegalArgumentException(
                    "Os limites dos ciclos de velocidade são inválidos."
            );
        }

        for (int grupo = 0; grupo < inicioPrimeiroCiclo.length; grupo++) {
            int inicioPrimeiro = inicioPrimeiroCiclo[grupo];
            int fimPrimeiro = fimPrimeiroCiclo[grupo];
            int inicioUltimo = inicioUltimoCiclo[grupo];
            int fimUltimo = fimUltimoCiclo[grupo];
            if (indiceQuadro < inicioPrimeiro ||
                    indiceQuadro > fimUltimo) {
                continue;
            }

            if (indiceQuadro <= fimPrimeiro) {
                int quantidadeRampa = Math.min(
                        QUADROS_DESACELERACAO,
                        fimPrimeiro - inicioPrimeiro + 1
                );
                int deslocamento = indiceQuadro - inicioPrimeiro;
                if (deslocamento < quantidadeRampa) {
                    if (quantidadeRampa == 1) {
                        return 1f;
                    }
                    return 1f -
                            (1f - VELOCIDADE_FINAL) * deslocamento /
                                    (quantidadeRampa - 1f);
                }
                return VELOCIDADE_FINAL;
            }

            if (indiceQuadro >= inicioUltimo) {
                int quantidadeRampa = Math.min(
                        QUADROS_DESACELERACAO,
                        fimUltimo - inicioUltimo + 1
                );
                int inicioRampa = fimUltimo - quantidadeRampa + 1;
                if (indiceQuadro >= inicioRampa) {
                    if (quantidadeRampa == 1) {
                        return 1f;
                    }
                    return VELOCIDADE_FINAL +
                            (1f - VELOCIDADE_FINAL) *
                                    (indiceQuadro - inicioRampa) /
                                    (quantidadeRampa - 1f);
                }
                return VELOCIDADE_FINAL;
            }
            return VELOCIDADE_FINAL;
        }
        return 1f;
    }

    public long getDuracaoQuadroMs(int indiceQuadro) {
        long duracaoNormal = Math.max(
                1L,
                durationMs / Math.max(1, frames.length)
        );
        return Math.max(
                1L,
                Math.round(duracaoNormal / getFatorVelocidade(indiceQuadro))
        );
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
