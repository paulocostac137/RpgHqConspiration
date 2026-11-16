package com.example.rpghqconspiration;

public final class GifSessionState {

    private boolean controleManualAtivo = false;
    private boolean controleRemotoAtivo = false;
    private boolean carregando = false;

    public boolean isManualAtivo() {
        return controleManualAtivo;
    }

    public void setManualAtivo(boolean ativo) {
        this.controleManualAtivo = ativo;
    }

    public boolean isRemotoAtivo() {
        return controleRemotoAtivo;
    }

    public void setRemotoAtivo(boolean ativo) {
        this.controleRemotoAtivo = ativo;
    }

    public boolean isCarregando() {
        return carregando;
    }

    public void setCarregando(boolean carregando) {
        this.carregando = carregando;
    }

    public boolean podeAvancarAutomaticamente() {
        return !controleManualAtivo && !controleRemotoAtivo && !carregando;
    }

    public void resetarControles() {
        controleManualAtivo = false;
        controleRemotoAtivo = false;
    }
}
