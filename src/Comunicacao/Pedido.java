package src.Comunicacao;

public class Pedido extends Comunicado {
    private final byte[] numeros;

    public Pedido(byte[] numeros) {
        this.numeros = numeros;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    /**
     * Ordena o vetor usando Merge Sort
     * @return vetor ordenado
     */
    public byte[] ordenar() {
        if (numeros == null || numeros.length <= 1) {
            return numeros;
        }
        return mergeSort(numeros, 0, numeros.length - 1);
    }

    /**
     * Implementação recursiva do Merge Sort
     */
    private byte[] mergeSort(byte[] arr, int inicio, int fim) {
        if (inicio >= fim) {
            return new byte[]{arr[inicio]};
        }

        int meio = inicio + (fim - inicio) / 2;

        // Ordena recursivamente cada metade
        byte[] esquerda = mergeSort(arr, inicio, meio);
        byte[] direita = mergeSort(arr, meio + 1, fim);

        // Faz o merge das duas metades ordenadas
        return merge(esquerda, direita);
    }

    /**
     * Faz o merge (intercalação) de dois vetores ordenados
     */
    private byte[] merge(byte[] esquerda, byte[] direita) {
        byte[] resultado = new byte[esquerda.length + direita.length];
        int i = 0, j = 0, k = 0;

        // Compara e intercala elementos
        while (i < esquerda.length && j < direita.length) {
            if (esquerda[i] <= direita[j]) {
                resultado[k++] = esquerda[i++];
            } else {
                resultado[k++] = direita[j++];
            }
        }

        // Copia elementos restantes de esquerda (se houver)
        while (i < esquerda.length) {
            resultado[k++] = esquerda[i++];
        }

        // Copia elementos restantes de direita (se houver)
        while (j < direita.length) {
            resultado[k++] = direita[j++];
        }

        return resultado;
    }
}


