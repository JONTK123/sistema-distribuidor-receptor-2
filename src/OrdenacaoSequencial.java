package src;

import java.io.*;
import java.security.SecureRandom;
import java.util.Scanner;
import src.Comunicacao.Pedido;

/**
 * Programa que faz a ordenação sem paralelismo/distribuição,
 * para comparar tempos com o sistema distribuído.
 */
public class OrdenacaoSequencial {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("=== ORDENAÇÃO SEQUENCIAL (SEM PARALELISMO) ===");
            
            // Solicitar tamanho do vetor
            System.out.print("\nDigite o tamanho do vetor (ex: 1000, 10000, 100000): ");
            int tamanho = scanner.nextInt();

            if (tamanho <= 0) {
                System.err.println("[ERRO] Tamanho inválido. Encerrando.");
                return;
            }

            System.out.println("[LOG] Gerando vetor de " + tamanho + " elementos...");

            // Geração do vetor
            SecureRandom rnd = new SecureRandom();
            byte[] vetor = new byte[tamanho];
            rnd.nextBytes(vetor);

            System.out.println("[LOG] Vetor gerado com sucesso!");

            // Perguntar se deseja exibir o vetor original
            System.out.print("\nDeseja exibir o vetor original na tela? (s/n): ");
            String resposta = scanner.next();
            if (resposta.equalsIgnoreCase("s")) {
                exibirVetor(vetor, "VETOR ORIGINAL");
            }

            // Ordenação sequencial
            System.out.println("\n[LOG] Iniciando ordenação sequencial (Merge Sort)...");
            long inicio = System.currentTimeMillis();
            byte[] vetorOrdenado = ordenarSequencial(vetor);
            long fim = System.currentTimeMillis();
            long tempoOrdenacao = fim - inicio;

            System.out.println("\n[RESULTADO] Ordenação sequencial concluída em " + tempoOrdenacao + " ms");

            // Validação
            System.out.println("[LOG] Validando ordenação...");
            boolean ordenadoCorretamente = verificarOrdenacao(vetorOrdenado);
            
            if (ordenadoCorretamente) {
                System.out.println("[SUCESSO] Vetor ordenado corretamente!");
            } else {
                System.err.println("[ERRO] Vetor NÃO está ordenado corretamente!");
            }

            // Perguntar se deseja exibir o vetor ordenado
            System.out.print("\nDeseja exibir o vetor ordenado na tela? (s/n): ");
            resposta = scanner.next();
            if (resposta.equalsIgnoreCase("s")) {
                exibirVetor(vetorOrdenado, "VETOR ORDENADO");
            }

            // Perguntar se deseja salvar em arquivo
            System.out.print("\nDeseja salvar o vetor ordenado em arquivo? (s/n): ");
            resposta = scanner.next();
            if (resposta.equalsIgnoreCase("s")) {
                System.out.print("Digite o nome do arquivo (ex: resultado_sequencial.txt): ");
                String nomeArquivo = scanner.next();
                salvarVetorEmArquivo(vetorOrdenado, nomeArquivo);
            }

            // Exibir estatísticas
            System.out.println("\n=== ESTATÍSTICAS ===");
            System.out.println("Tamanho do vetor: " + tamanho);
            System.out.println("Tempo de ordenação sequencial: " + tempoOrdenacao + " ms");
            System.out.println("Tempo médio por elemento: " + String.format("%.6f", tempoOrdenacao / (double)tamanho) + " ms");
            System.out.println("\nDica: Execute o Distribuidor com o mesmo tamanho de vetor para comparar os tempos!");

        } catch (Exception e) {
            System.err.println("[ERRO] Exceção capturada: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            System.out.println("\n=== FIM DA ORDENAÇÃO SEQUENCIAL ===");
        }
    }

    /**
     * Ordena o vetor usando Merge Sort (sem paralelismo)
     */
    private static byte[] ordenarSequencial(byte[] vetor) {
        Pedido pedido = new Pedido(vetor);
        return pedido.ordenar();
    }

    /**
     * Verifica se um vetor está ordenado corretamente
     */
    private static boolean verificarOrdenacao(byte[] vetor) {
        for (int i = 0; i < vetor.length - 1; i++) {
            if (vetor[i] > vetor[i + 1]) {
                System.err.println("[ERRO] Falha na ordenação no índice " + i + 
                        ": " + vetor[i] + " > " + vetor[i + 1]);
                return false;
            }
        }
        return true;
    }

    /**
     * Exibe um vetor na tela (útil para depuração com vetores pequenos)
     */
    private static void exibirVetor(byte[] vetor, String titulo) {
        System.out.println("\n[" + titulo + "]");
        int limite = Math.min(vetor.length, 200); // Limita exibição a 200 elementos
        for (int i = 0; i < limite; i++) {
            System.out.print(vetor[i]);
            if (i < limite - 1) System.out.print(", ");
            if ((i + 1) % 20 == 0) System.out.println();
        }
        if (vetor.length > limite) {
            System.out.println("\n... (" + (vetor.length - limite) + " elementos omitidos)");
        }
        System.out.println("\n");
    }

    /**
     * Salva o vetor ordenado em um arquivo de texto
     */
    private static void salvarVetorEmArquivo(byte[] vetor, String nomeArquivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(nomeArquivo))) {
            for (int i = 0; i < vetor.length; i++) {
                writer.print(vetor[i]);
                if (i < vetor.length - 1) {
                    writer.print(", ");
                }
                if ((i + 1) % 20 == 0) {
                    writer.println();
                }
            }
            System.out.println("[LOG] Vetor salvo com sucesso em: " + nomeArquivo);
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao salvar arquivo: " + e.getMessage());
        }
    }
}


