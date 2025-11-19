package src.Comunicacao;

public class Resposta extends Comunicado {

    private final byte[] vetorOrdenado;

    public Resposta(byte[] vetorOrdenado) {
        this.vetorOrdenado = vetorOrdenado;
    }

    public byte[] getVetor() {
        return vetorOrdenado;
    }
}


