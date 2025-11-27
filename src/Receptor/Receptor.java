package src.Receptor;

import java.io.*;
import java.net.*;
import java.util.*;
import src.Comunicacao.*;

public class Receptor {

    private static final int NUM_PROCESSADORES = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        int porta = 0;

        try {
            // Permitir escolher a porta via argumento de linha de comando
            if (args.length > 0) {
                porta = Integer.parseInt(args[0]);
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Digite a porta para o receptor (ex: 12345, 12346): ");
                porta = scanner.nextInt();
                scanner.close();
            }

            serverSocket = new ServerSocket(porta);
            System.out.println("=== RECEPTOR INICIADO ===");
            System.out.println("[LOG] Servidor rodando na porta: " + serverSocket.getLocalPort());
            System.out.println("[LOG] Processadores disponíveis: " + NUM_PROCESSADORES);
            System.out.println("[LOG] Aguardando conexões...\n");

            int numeroConexao = 0;

            // Loop principal que aceita conexões
            while (true) {
                Socket conexao = serverSocket.accept();
                final int idConexao = ++numeroConexao;

                System.out.println("[LOG] Conexão #" + idConexao + " aceita de: " +
                        conexao.getInetAddress().getHostAddress() + ":" + conexao.getPort());

                // Cria thread para tratar a conexão
                Thread threadConexao = new Thread(() -> {
                    tratarConexao(conexao, idConexao);
                }, "Thread-Conexao-" + idConexao);

                threadConexao.start();
            }

        } catch (NumberFormatException e) {
            System.err.println("[ERRO FATAL] Porta inválida fornecida");
        } catch (IOException e) {
            System.err.println("[ERRO FATAL] Erro ao criar ServerSocket na porta " + porta + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERRO FATAL] Exceção no main: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("[LOG] ServerSocket fechado");
                } catch (IOException e) {
                    System.err.println("[ERRO] Ao fechar ServerSocket: " + e.getMessage());
                }
            }
            System.out.println("=== RECEPTOR ENCERRADO ===");
        }
    }

    /**
     * Trata uma conexão individual
     */
    private static void tratarConexao(Socket conexao, int idConexao) {
        ObjectInputStream receptor = null;
        ObjectOutputStream transmissor = null;

        try {
            System.out.println("[LOG] Conexão #" + idConexao + " - Inicializando streams...");

            receptor = new ObjectInputStream(conexao.getInputStream());
            transmissor = new ObjectOutputStream(conexao.getOutputStream());
            transmissor.flush();

            System.out.println("[LOG] Conexão #" + idConexao + " - Pronto para receber pedidos");

            Object objeto;
            int numeroPedido = 0;

            // Loop de processamento de pedidos
            while (true) {
                objeto = receptor.readObject();

                if (objeto instanceof Pedido) {
                    numeroPedido++;
                    Pedido pedidoRecebido = (Pedido) objeto;

                    System.out.println("[LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                            " recebido (tamanho vetor: " + pedidoRecebido.getNumeros().length + ")");

                    long inicio = System.currentTimeMillis();
                    byte[] vetorOrdenado = mergeSortParaleloRecursivo(
                            pedidoRecebido.getNumeros(),
                            0,
                            idConexao,
                            numeroPedido
                    );
                    long fim = System.currentTimeMillis();

                    Resposta resposta = new Resposta(vetorOrdenado);
                    transmissor.writeObject(resposta);
                    transmissor.flush();

                    System.out.println("[LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                            " processado e respondido em " + (fim - inicio) + " ms");
                }
                else if (objeto instanceof ComunicadoEncerramento) {
                    System.out.println("[LOG] Conexão #" + idConexao +
                            " - ComunicadoEncerramento recebido");
                    break;
                }
                else {
                    System.err.println("[ERRO] Conexão #" + idConexao +
                            " - Objeto desconhecido recebido: " + objeto.getClass().getName());
                }
            }

            System.out.println("[LOG] Conexão #" + idConexao + " - Encerrando...");

        } catch (EOFException e) {
            System.err.println("[ERRO] Conexão #" + idConexao +
                    " - Fim inesperado do stream (cliente desconectou?)");
        } catch (SocketException e) {
            System.err.println("[ERRO] Conexão #" + idConexao +
                    " - Erro de socket: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[ERRO] Conexão #" + idConexao +
                    " - Classe não encontrada: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERRO] Conexão #" + idConexao +
                    " - Exceção não esperada: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Fechamento seguro dos recursos
            try {
                if (transmissor != null) {
                    transmissor.close();
                    System.out.println("[LOG] Conexão #" + idConexao + " - Transmissor fechado");
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Conexão #" + idConexao +
                        " - Erro ao fechar transmissor: " + e.getMessage());
            }

            try {
                if (receptor != null) {
                    receptor.close();
                    System.out.println("[LOG] Conexão #" + idConexao + " - Receptor fechado");
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Conexão #" + idConexao +
                        " - Erro ao fechar receptor: " + e.getMessage());
            }

            try {
                if (conexao != null && !conexao.isClosed()) {
                    conexao.close();
                    System.out.println("[LOG] Conexão #" + idConexao + " - Socket fechado\n");
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Conexão #" + idConexao +
                        " - Erro ao fechar socket: " + e.getMessage());
            }
        }
    }

    /**
     * Merge Sort Paralelo Recursivo
     * @param vetor - vetor a ser ordenado
     * @param profundidade - nível de recursão (para controlar paralelismo)
     * @param idConexao - ID da conexão (para logs)
     * @param numeroPedido - número do pedido (para logs)
     * @return vetor ordenado
     */
    private static byte[] mergeSortParaleloRecursivo(byte[] vetor, int profundidade,
                                                     int idConexao, int numeroPedido)
            throws InterruptedException {

        int tamanho = vetor.length;

        // Caso base: vetor pequeno ou muita profundidade, ordena sequencialmente
        if (tamanho <= 1000 || profundidade >= 10) {
            if (profundidade == 0) {
                System.out.println("    [LOG] Vetor pequeno, ordenando sequencialmente");
            }
            Pedido p = new Pedido(vetor);
            return p.ordenar();
        }

        // Log apenas na primeira chamada
        if (profundidade == 0) {
            System.out.println("    [LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                    " - Iniciando Merge Sort paralelo recursivo");
        }

        // Divide o vetor ao meio
        int meio = tamanho / 2;
        byte[] esquerda = Arrays.copyOfRange(vetor, 0, meio);
        byte[] direita = Arrays.copyOfRange(vetor, meio, tamanho);

        // Decide se usa paralelismo baseado na profundidade
        // Só paraleliza nos primeiros níveis para não criar threads demais
        boolean usarParalelismo = profundidade < Math.log(NUM_PROCESSADORES) / Math.log(2);

        byte[] esquerdaOrdenada;
        byte[] direitaOrdenada;

        if (usarParalelismo) {
            // Cria threads para processar cada metade recursivamente
            ThreadMergeSortRecursivo threadEsquerda = new ThreadMergeSortRecursivo(
                    esquerda, profundidade + 1, idConexao, numeroPedido, "esq");
            ThreadMergeSortRecursivo threadDireita = new ThreadMergeSortRecursivo(
                    direita, profundidade + 1, idConexao, numeroPedido, "dir");

            if (profundidade == 0) {
                System.out.println("    [LOG] Dividindo em 2 threads recursivas (prof=" + profundidade + ")");
            }

            threadEsquerda.start();
            threadDireita.start();

            threadEsquerda.join();
            threadDireita.join();

            esquerdaOrdenada = threadEsquerda.getResultado();
            direitaOrdenada = threadDireita.getResultado();
        } else {
            // Recursão sequencial nos níveis mais profundos
            esquerdaOrdenada = mergeSortParaleloRecursivo(esquerda, profundidade + 1, idConexao, numeroPedido);
            direitaOrdenada = mergeSortParaleloRecursivo(direita, profundidade + 1, idConexao, numeroPedido);
        }

        // Faz merge dos resultados ordenados
        byte[] resultado = merge(esquerdaOrdenada, direitaOrdenada);

        if (profundidade == 0) {
            System.out.println("    [LOG] Merge Sort recursivo completo");
        }

        return resultado;
    }

    /**
     * Faz o merge (intercalação) de dois vetores ordenados
     */
    private static byte[] merge(byte[] esquerda, byte[] direita) {
        byte[] resultado = new byte[esquerda.length + direita.length];
        int i = 0, j = 0, k = 0;

        while (i < esquerda.length && j < direita.length) {
            if (esquerda[i] <= direita[j]) {
                resultado[k++] = esquerda[i++];
            } else {
                resultado[k++] = direita[j++];
            }
        }

        while (i < esquerda.length) {
            resultado[k++] = esquerda[i++];
        }

        while (j < direita.length) {
            resultado[k++] = direita[j++];
        }

        return resultado;
    }

    /**
     * Thread que executa Merge Sort recursivo em um subvetor
     */
    private static class ThreadMergeSortRecursivo extends Thread {
        private final byte[] vetor;
        private final int profundidade;
        private final int idConexao;
        private final int numeroPedido;
        private final String lado;
        private byte[] resultado;

        public ThreadMergeSortRecursivo(byte[] vetor, int profundidade,
                                        int idConexao, int numeroPedido, String lado) {
            super("Thread-MergeSort-" + lado + "-Prof" + profundidade);
            this.vetor = vetor;
            this.profundidade = profundidade;
            this.idConexao = idConexao;
            this.numeroPedido = numeroPedido;
            this.lado = lado;
        }

        @Override
        public void run() {
            try {
                resultado = mergeSortParaleloRecursivo(vetor, profundidade, idConexao, numeroPedido);
            } catch (InterruptedException e) {
                System.err.println("[ERRO] Thread interrompida: " + getName());
                Thread.currentThread().interrupt();
            }
        }

        public byte[] getResultado() {
            return resultado;
        }
    }
}