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
                    byte[] vetorOrdenado = ordenarComThreads(
                                        pedidoRecebido.getNumeros(),
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
     * Ordena o vetor usando múltiplas threads e depois faz merge dos resultados
     */
    private static byte[] ordenarComThreads(byte[] vetor, int idConexao, int numeroPedido) 
            throws InterruptedException {
        
        int tamanho = vetor.length;
        
        // Se o vetor for muito pequeno, ordena sequencialmente
        if (tamanho <= 1000) {
            System.out.println("    [LOG] Vetor pequeno, ordenando sequencialmente");
            Pedido p = new Pedido(vetor);
            return p.ordenar();
        }

        // Divide o trabalho entre as threads ordenadoras
        int numThreads = Math.min(NUM_PROCESSADORES, tamanho);
        int tamanhoParte = tamanho / numThreads;
        
        System.out.println("    [LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                " - Iniciando ordenação com " + numThreads + " threads");

        ThreadOrdenadora[] threads = new ThreadOrdenadora[numThreads];
        
        // Cria e inicia threads ordenadoras
        for (int i = 0; i < numThreads; i++) {
            int inicio = i * tamanhoParte;
            int fim = (i == numThreads - 1) ? tamanho : (i + 1) * tamanhoParte;
            
            byte[] subVetor = Arrays.copyOfRange(vetor, inicio, fim);
            threads[i] = new ThreadOrdenadora(subVetor, i);
            threads[i].start();
            
            System.out.println("    [LOG] Thread ordenadora " + i + " iniciada para índices [" 
                    + inicio + ", " + fim + ")");
        }

        // Aguarda todas as threads ordenadoras terminarem
        byte[][] resultadosParciais = new byte[numThreads][];
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
            resultadosParciais[i] = threads[i].getResultado();
            System.out.println("    [LOG] Thread ordenadora " + i + " finalizou");
        }

        // Faz merge dos resultados usando threads juntadoras
        System.out.println("    [LOG] Iniciando processo de merge dos resultados");
        byte[] resultado = mergeComThreads(resultadosParciais, idConexao, numeroPedido);
        
        System.out.println("    [LOG] Ordenação completa concluída");
        return resultado;
    }

    /**
     * Faz merge de múltiplos vetores ordenados usando threads juntadoras
     */
    private static byte[] mergeComThreads(byte[][] vetores, int idConexao, int numeroPedido) 
            throws InterruptedException {

        List<byte[]> lista = new ArrayList<>(Arrays.asList(vetores));
        int rodada = 1;

        // Enquanto houver mais de um vetor, continua fazendo merge 2 a 2
        while (lista.size() > 1) {
            System.out.println("    [LOG] Rodada de merge #" + rodada + " - " + lista.size() + " vetores");

            List<byte[]> novaLista = new ArrayList<>();
            int numPares = lista.size() / 2;
            int numThreads = Math.min(NUM_PROCESSADORES, numPares);
            
            // Se temos pares suficientes, usa threads para merge paralelo
            if (numPares > 0 && numThreads > 0) {
                // Calcula quantos pares cada thread vai processar
                int paresPorThread = (numPares + numThreads - 1) / numThreads;
                
                ThreadJuntadora[] threadsJuntadoras = new ThreadJuntadora[numThreads];

                for (int t = 0; t < numThreads; t++) {
                    int inicioPar = t * paresPorThread * 2;
                    int fimPar = Math.min((t + 1) * paresPorThread * 2, lista.size() - (lista.size() % 2));
                    
                    if (inicioPar < fimPar) {
                        List<byte[]> subLista = lista.subList(inicioPar, fimPar);
                        threadsJuntadoras[t] = new ThreadJuntadora(subLista, t);
                        threadsJuntadoras[t].start();
                        System.out.println("    [LOG] Thread juntadora " + t + " iniciada");
                    }
                }
                
                // Aguarda threads juntadoras
                for (int t = 0; t < numThreads; t++) {
                    if (threadsJuntadoras[t] != null) {
                        threadsJuntadoras[t].join();
                        novaLista.addAll(threadsJuntadoras[t].getResultados());
                        System.out.println("    [LOG] Thread juntadora " + t + " finalizou");
                    }
                }
            }
            
            // Se sobrou um vetor ímpar, adiciona-o
            if (lista.size() % 2 == 1) {
                novaLista.add(lista.get(lista.size() - 1));
                System.out.println("    [LOG] Vetor ímpar adicionado para próxima rodada");
            }
            
            lista = novaLista;
            rodada++;
        }

        return lista.get(0);
    }

    /**
     * Thread que ordena uma parte do vetor
     */
    private static class ThreadOrdenadora extends Thread {
        private final byte[] vetor;
        private final int id;
        private byte[] resultado;

        public ThreadOrdenadora(byte[] vetor, int id) {
            super("Thread-Ordenadora-" + id);
            this.vetor = vetor;
            this.id = id;
        }

        @Override
        public void run() {
            Pedido pedido = new Pedido(vetor);
            resultado = pedido.ordenar();
        }

        public byte[] getResultado() {
            return resultado;
        }
    }

    /**
     * Thread que faz merge de pares de vetores ordenados
     */
    private static class ThreadJuntadora extends Thread {
        private final List<byte[]> vetores;
        private final int id;
        private final List<byte[]> resultados;

        public ThreadJuntadora(List<byte[]> vetores, int id) {
            super("Thread-Juntadora-" + id);
            this.vetores = vetores;
            this.id = id;
            this.resultados = new ArrayList<>();
        }

        @Override
        public void run() {
            // Processa pares de vetores
            for (int i = 0; i < vetores.size() - 1; i += 2) {
                byte[] merged = merge(vetores.get(i), vetores.get(i + 1));
                resultados.add(merged);
            }
            
            // Se sobrou um vetor ímpar nesta sublista
            if (vetores.size() % 2 == 1) {
                resultados.add(vetores.get(vetores.size() - 1));
            }
        }

        public List<byte[]> getResultados() {
            return resultados;
        }

        /**
         * Faz o merge (intercalação) de dois vetores ordenados
         */
        private byte[] merge(byte[] esquerda, byte[] direita) {
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
    }
}
