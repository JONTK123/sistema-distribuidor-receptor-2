package src.Distribuidor;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import src.Comunicacao.*;

public class Distribuidor {

    private static final int NUM_PROCESSADORES = Runtime.getRuntime().availableProcessors();

    static class ConexaoR {
        String host;
        int porta;
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;
        private final Lock mutex = new ReentrantLock();

        public ConexaoR(String host, int porta) throws IOException {
            this.host = host;
            this.porta = porta;
            this.socket = new Socket(host, porta);
            this.socket.setTcpNoDelay(true);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[LOG] Conectado a " + host + ":" + porta);
        }

        public Resposta enviarPedido(Pedido pedido) throws IOException, ClassNotFoundException {
            mutex.lock();
            try {
                out.writeObject(pedido);
                out.flush();
                System.out.println("[LOG] Pedido enviado para " + this);

                Object obj = in.readObject();
                if (obj instanceof Resposta) {
                    System.out.println("[LOG] Resposta recebida de " + this);
                    return (Resposta) obj;
                }
                throw new IOException("Resposta inválida recebida de " + this);
            } finally {
                mutex.unlock();
            }
        }

        public void enviarEncerramento() throws IOException {
            mutex.lock();
            try {
                System.out.println("[LOG] Enviando encerramento para " + this);
                out.writeObject(new ComunicadoEncerramento());
                out.flush();
            } finally {
                mutex.unlock();
            }
        }

        public void fechar() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[LOG] Conexão fechada com " + this);
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Ao fechar conexão com " + this + ": " + e.getMessage());
            }
        }

        @Override
        public String toString() {
            return host + ":" + porta;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<ConexaoR> conexoes = new ArrayList<>();

        try {
            System.out.println("=== INICIANDO DISTRIBUIDOR ===");
            System.out.println("[LOG] Processadores disponíveis: " + NUM_PROCESSADORES);

            // IPs e portas hard coded
            String[] servidores = {
                    "localhost:12345",
                    "localhost:12346"
            };

            // Criação das conexões persistentes
            System.out.println("\n[LOG] Estabelecendo conexões com os receptores...");
            for (String s : servidores) {
                try {
                    String[] partes = s.split(":");
                    ConexaoR conexao = new ConexaoR(partes[0], Integer.parseInt(partes[1]));
                    conexoes.add(conexao);
                } catch (IOException e) {
                    System.err.println("[ERRO] Não foi possível conectar a " + s + ": " + e.getMessage());
                    System.err.println("[AVISO] Verifique se o servidor está rodando nesta porta!");
                }
            }

            if (conexoes.isEmpty()) {
                System.err.println("[ERRO FATAL] Nenhuma conexão estabelecida. Encerrando.");
                return;
            }

            System.out.println("[LOG] Total de conexões estabelecidas: " + conexoes.size() + "/" + servidores.length);

            // Solicitar tamanho do vetor
            System.out.print("\nDigite o tamanho do vetor (ex: 1000, 10000, 100000): ");
            int TAM = scanner.nextInt();

            if (TAM <= 0) {
                System.err.println("[ERRO] Tamanho inválido. Encerrando.");
                return;
            }

            System.out.println("[LOG] Gerando vetor de " + TAM + " elementos...");

            // Geração do vetor principal
            SecureRandom rnd = new SecureRandom();
            byte[] vetor = new byte[TAM];
            rnd.nextBytes(vetor); // Mais eficiente para preencher com bytes aleatórios

            System.out.println("[LOG] Vetor gerado com sucesso!");

            // Perguntar se deseja exibir o vetor
            System.out.print("\nDeseja exibir o vetor original na tela? (s/n): ");
            String resposta = scanner.next();
            if (resposta.equalsIgnoreCase("s")) {
                exibirVetor(vetor, "VETOR ORIGINAL");
            }

            // Ordenação distribuída principal
            System.out.println("\n[LOG] Iniciando ordenação distribuída...");
            long inicio = System.currentTimeMillis();
            byte[] vetorOrdenado = ordenarDistribuido(conexoes, vetor);
            long fim = System.currentTimeMillis();
            long tempoDistribuido = fim - inicio;
            
            System.out.println("\n[RESULTADO] Ordenação distribuída concluída em " + tempoDistribuido + " ms");

            // Validação: verificar se está realmente ordenado
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
                System.out.print("Digite o nome do arquivo (ex: resultado.txt): ");
                String nomeArquivo = scanner.next();
                salvarVetorEmArquivo(vetorOrdenado, nomeArquivo);
            }

            // Exibir estatísticas
            System.out.println("\n=== ESTATÍSTICAS ===");
            System.out.println("Tamanho do vetor: " + TAM);
            System.out.println("Número de receptores: " + conexoes.size());
            System.out.println("Tempo de ordenação distribuída: " + tempoDistribuido + " ms");
            System.out.println(String.format("Tempo médio por elemento: %.6f ms", tempoDistribuido / (double)TAM));

        } catch (InputMismatchException e) {
            System.err.println("[ERRO] Entrada inválida. Por favor, digite um número inteiro.");
        } catch (Exception e) {
            System.err.println("[ERRO] Exceção capturada no main: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Envio do ComunicadoEncerramento e fechamento de conexões
            System.out.println("\n[LOG] Encerrando conexões...");
            for (ConexaoR c : conexoes) {
                try {
                    c.enviarEncerramento();
                    c.fechar();
                } catch (IOException e) {
                    System.err.println("[ERRO] Ao enviar encerramento para " + c + ": " + e.getMessage());
                }
            }

            scanner.close();
            System.out.println("\n=== FIM DO DISTRIBUIDOR ===");
        }
    }

    /**
     * Realiza a ordenação distribuída enviando pedidos em paralelo
     */
    private static byte[] ordenarDistribuido(List<ConexaoR> conexoes, byte[] vetor) throws Exception {
        List<Thread> threads = new ArrayList<>();
        List<Resposta> respostas = Collections.synchronizedList(new ArrayList<>());
        List<Exception> excecoes = Collections.synchronizedList(new ArrayList<>());

        int tamanhoParte = vetor.length / conexoes.size();
        int resto = vetor.length % conexoes.size();

        System.out.println("[LOG] Dividindo vetor em " + conexoes.size() + " partes...");

        // Cria threads para enviar pedidos aos receptores
        for (int i = 0; i < conexoes.size(); i++) {
            final int inicio = i * tamanhoParte;
            final int fim = inicio + tamanhoParte + (i == conexoes.size() - 1 ? resto : 0);
            final byte[] subVetor = Arrays.copyOfRange(vetor, inicio, fim);

            final int indice = i;
            Thread thread = new Thread(() -> {
                try {
                    ConexaoR c = conexoes.get(indice);
                    System.out.println("[LOG] Thread-" + indice + " enviando para " + c +
                            " (tam=" + subVetor.length + ")");

                    Pedido pedido = new Pedido(subVetor);
                    Resposta r = c.enviarPedido(pedido);

                    respostas.add(r);
                    System.out.println("[LOG] Thread-" + indice + " recebeu resposta de " + c +
                            " (vetor ordenado de tamanho " + r.getVetor().length + ")");
                } catch (Exception e) {
                    System.err.println("[ERRO] Thread-" + indice + " falhou ao comunicar com " +
                            conexoes.get(indice) + ": " + e.getMessage());
                    excecoes.add(e);
                }
            }, "Thread-Receptor-" + i);

            threads.add(thread);
            thread.start();
            System.out.println("[LOG] Thread-" + indice + " iniciada");
        }

        // Sincronização das threads usando join()
        System.out.println("[LOG] Aguardando conclusão de todas as threads...");
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
                System.out.println("[LOG] Thread-" + i + " finalizada");
            } catch (InterruptedException e) {
                System.err.println("[ERRO] Thread-" + i + " interrompida: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // Verificar se houve exceções
        if (!excecoes.isEmpty()) {
            System.err.println("[AVISO] " + excecoes.size() + " thread(s) falharam durante a execução");
            throw new Exception("Falhas na comunicação com receptores: " + excecoes.size() + " erros");
        }

        // Extrai vetores ordenados das respostas
        System.out.println("[LOG] Extraindo vetores ordenados das respostas...");
        byte[][] vetoresOrdenados = new byte[respostas.size()][];
        for (int i = 0; i < respostas.size(); i++) {
            vetoresOrdenados[i] = respostas.get(i).getVetor();
        }

        // Faz merge dos vetores ordenados usando threads
        System.out.println("[LOG] Iniciando merge dos vetores ordenados...");
        byte[] resultado = mergeComThreads(vetoresOrdenados);
        
        return resultado;
    }

    /**
     * Faz merge de múltiplos vetores ordenados usando threads juntadoras
     */
    private static byte[] mergeComThreads(byte[][] vetores) throws InterruptedException {
        List<byte[]> lista = new ArrayList<>(Arrays.asList(vetores));
        int rodada = 1;

        // Enquanto houver mais de um vetor, continua fazendo merge 2 a 2
        while (lista.size() > 1) {
            System.out.println("[LOG] Rodada de merge #" + rodada + " - " + lista.size() + " vetores");
            
            List<byte[]> novaLista = new ArrayList<>();
            int numPares = lista.size() / 2;
            int numThreads = Math.min(NUM_PROCESSADORES, numPares);
            
            // Se temos pares suficientes, usa threads para merge paralelo
            if (numPares > 0 && numThreads > 0) {
                // Calcula quantos pares cada thread vai processar
                int paresPorThread = Math.max(1, numPares / numThreads);
                List<ThreadJuntadora> threadsJuntadoras = new ArrayList<>();
                
                int parInicial = 0;
                for (int t = 0; t < numThreads && parInicial < numPares; t++) {
                    int inicioPar = parInicial * 2;
                    int numParesThread = Math.min(paresPorThread, numPares - parInicial);
                    int fimPar = inicioPar + numParesThread * 2;
                    
                    List<byte[]> subLista = lista.subList(inicioPar, fimPar);
                    ThreadJuntadora tj = new ThreadJuntadora(subLista, t);
                    tj.start();
                    threadsJuntadoras.add(tj);
                    
                    System.out.println("[LOG] Thread juntadora " + t + " iniciada para processar " 
                            + numParesThread + " par(es)");
                    
                    parInicial += numParesThread;
                }
                
                // Aguarda threads juntadoras
                for (ThreadJuntadora tj : threadsJuntadoras) {
                    tj.join();
                    novaLista.addAll(tj.getResultados());
                    System.out.println("[LOG] Thread juntadora " + tj.id + " finalizou");
                }
            }
            
            // Se sobrou um vetor ímpar, adiciona-o
            if (lista.size() % 2 == 1) {
                novaLista.add(lista.get(lista.size() - 1));
                System.out.println("[LOG] Vetor ímpar adicionado para próxima rodada");
            }
            
            lista = novaLista;
            rodada++;
        }

        return lista.get(0);
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
        for (int i = 0; i < vetor.length; i++) {
            System.out.print(vetor[i]);
            if (i < vetor.length - 1) System.out.print(", ");
            if ((i + 1) % 20 == 0) System.out.println();
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
