package com.example.rpghqconspiration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GifCycleAlignmentTest {

    @Test
    public void desaceleraGradualmenteEPermaneceAbaixoDaVelocidadeNatural() {
        GifCycleAlignment alignment = new GifCycleAlignment();
        alignment.begin(24, 800L, 0, 12, 0L);

        float speedAfterFirstStep = alignment.updateSpeed(0, 100L);
        float speedAfterSecondStep = alignment.updateSpeed(1, 200L);

        assertTrue(speedAfterFirstStep < 1f);
        assertTrue(speedAfterSecondStep < speedAfterFirstStep);
    }

    @Test
    public void consideraVelocidadeDoMovimentoDoControle() {
        GifCycleAlignment controleParado = new GifCycleAlignment();
        controleParado.begin(24, 800L, 0, 8, 0L);

        GifCycleAlignment controleEmMovimento = new GifCycleAlignment();
        controleEmMovimento.begin(24, 800L, 0, 6, 0L);
        controleEmMovimento.updateTarget(8, 100L);

        assertTrue(
                controleEmMovimento.updateSpeed(0, 100L) >
                        controleParado.updateSpeed(0, 100L)
        );
    }

    @Test
    public void reduzVelocidadeQuandoAlvoDoControleParaDeAtualizar() {
        GifCycleAlignment alignment = new GifCycleAlignment();
        alignment.begin(24, 800L, 0, 3, 0L);
        alignment.updateTarget(3, 100L);

        float enquantoControleAtualiza = alignment.updateSpeed(1, 200L);
        float aposControleParar = alignment.updateSpeed(2, 300L);

        assertTrue(aposControleParar < enquantoControleAtualiza);
    }

    @Test
    public void naoUltrapassaVelocidadeNaturalQuandoControleMoveRapido() {
        GifCycleAlignment alignment = new GifCycleAlignment();
        alignment.begin(24, 800L, 0, 1, 0L);
        alignment.updateTarget(20, 1L);

        assertTrue(alignment.updateSpeed(0, 100L) <= 1f);
    }

    @Test
    public void aceleraGradualmenteAteVelocidadeNaturalAposSoltarControle() {
        GifCycleAlignment alignment = new GifCycleAlignment();
        alignment.begin(24, 800L, 0, 12, 0L);
        float velocidadeReduzida = alignment.updateSpeed(0, 100L);
        alignment.beginRecovery(100L);

        float primeiraEtapa = alignment.updateRecovery(200L);
        float segundaEtapa = alignment.updateRecovery(300L);
        float velocidadeNatural = alignment.updateRecovery(10_000L);

        assertTrue(velocidadeReduzida < primeiraEtapa);
        assertTrue(primeiraEtapa < segundaEtapa);
        assertEquals(1f, velocidadeNatural, 0f);
    }
}
