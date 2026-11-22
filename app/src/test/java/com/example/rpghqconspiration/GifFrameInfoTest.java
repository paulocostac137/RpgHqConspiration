package com.example.rpghqconspiration;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class GifFrameInfoTest {

    @Test
    public void contaDescritoresECalculaDuracaoDoCiclo() throws Exception {
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(10, 20, 10))
        );

        assertEquals(3, info.getFrameCount());
        assertEquals(400L, info.getCycleDurationMs());
        assertEquals(12, info.getFrameCountAt30Fps());
        assertEquals(100, info.getFrameTimeMs(1, 400));
        assertEquals(300, info.getFrameTimeMs(2, 400));
    }

    @Test
    public void distribuiDuplicacoesUniformementeA30Fps() throws Exception {
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(50, 50))
        );
        int[] occurrences = new int[2];

        for (int i = 0; i < info.getFrameCountAt30Fps(); i++) {
            occurrences[info.getSourceFrameFor30FpsFrame(i)]++;
        }

        assertEquals(30, info.getFrameCountAt30Fps());
        assertEquals(15, occurrences[0]);
        assertEquals(15, occurrences[1]);
    }

    @Test
    public void repeteTodosOsQuadrosDoCicloPeloNumeroSolicitado()
            throws Exception {
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(80))
        );

        assertEquals(24, info.getFrameCountAt30Fps());
        assertEquals(72, info.getFrameCountAt30Fps(3));
    }

    @Test
    public void repeteSomenteAFaixaMarcadaCincoVezes() throws Exception {
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(10, 20, 10))
        );

        assertEquals(
                24,
                info.getFrameCountAt30Fps(
                        new FaixaRepeticaoGif(2, 4),
                        5
                )
        );
    }

    @Test
    public void calculaSeteRepeticoesParaCadaFaixaMarcada() throws Exception {
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(10, 20, 10))
        );
        FaixaRepeticaoGif faixas = FaixaRepeticaoGif.agrupar(
                java.util.Arrays.asList(
                        new FaixaRepeticaoGif(1, 1, 2),
                        new FaixaRepeticaoGif(2, 2, 3)
                )
        );

        assertEquals(37, info.getFrameCountAt30Fps(faixas, 7));
    }

    @Test
    public void distribuiReducaoDeQuadrosUniformemente() throws Exception {
        int[] delays = new int[40];
        for (int i = 0; i < delays.length; i++) {
            delays[i] = i < 20 ? 2 : 3;
        }
        GifFrameInfo info = GifFrameInfo.read(
                new ByteArrayInputStream(gifWithDelays(delays))
        );
        boolean[] selected = new boolean[40];

        for (int i = 0; i < info.getFrameCountAt30Fps(); i++) {
            selected[info.getSourceFrameFor30FpsFrame(i)] = true;
        }

        assertEquals(30, info.getFrameCountAt30Fps());
        int selectedCount = 0;
        for (boolean frameSelected : selected) {
            if (frameSelected) {
                selectedCount++;
            }
        }
        assertEquals(30, selectedCount);
    }

    @Test(expected = IOException.class)
    public void rejeitaArquivoQueNaoEGif() throws Exception {
        GifFrameInfo.read(new ByteArrayInputStream(
                new byte[]{'N', 'O', 'T', 'G', 'I', 'F'}
        ));
    }

    private byte[] gifWithDelays(int... delaysCentiseconds)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write("GIF89a".getBytes("US-ASCII"));
        output.write(new byte[]{
                1, 0, 1, 0, (byte) 0x80, 0, 0,
                0, 0, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        });

        for (int delay : delaysCentiseconds) {
            output.write(new byte[]{
                    0x21, (byte) 0xF9, 4, 0,
                    (byte) delay, (byte) (delay >> 8), 0, 0,
                    0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0,
                    2, 2, 0x44, 1, 0
            });
        }
        output.write(0x3B);
        return output.toByteArray();
    }
}
