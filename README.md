# Sistema Distribuído de Ordenação com Merge Sort

## 📋 Visão Geral

Este projeto implementa um sistema distribuído para **ordenação de grandes vetores** usando algoritmo **Merge Sort**, desenvolvido como trabalho prático da disciplina de Programação Paralela e Distribuída. O sistema utiliza arquitetura cliente-servidor com TCP/IP, serialização de objetos e processamento paralelo com **controle manual de threads** para otimizar a ordenação de grandes conjuntos de dados.

## 🎯 Objetivo

Desenvolver um sistema distribuído onde um programa **Distribuidor (D)** gera um grande vetor de números inteiros aleatórios (tipo `byte`), particiona o vetor em partes de tamanho aproximadamente iguais, e envia essas partes a diferentes programas **Receptores (R)**, que executam a ordenação em paralelo usando Merge Sort. Após ordenar, os vetores ordenados são intercalados (merged) para produzir o resultado final.

## 🏗️ Arquitetura do Sistema

O sistema é composto por três programas principais:

### 1. Distribuidor (D) - Cliente Coordenador
- Gera um vetor de bytes aleatórios de tamanho configurável
- Divide o vetor em partes e distribui para os Receptores
- Mantém conexões persistentes TCP/IP com múltiplos Receptores
- Recebe vetores ordenados dos Receptores
- Faz **merge paralelo** dos vetores ordenados usando threads juntadoras
- Valida se o vetor final está corretamente ordenado
- Salva o resultado em arquivo texto

### 2. Receptor (R) - Servidor de Ordenação
- Aguarda conexões de clientes em porta configurável
- Recebe pedidos de ordenação via objetos serializados
- **Usa controle manual de threads** (sem thread pool)
- Divide o trabalho entre **threads ordenadoras** (uma por processador)
- Cada thread ordena sua parte usando **Merge Sort recursivo**
- **Threads juntadoras** fazem merge dos resultados 2 a 2
- Retorna vetor ordenado ao Distribuidor
- Mantém conexão aberta até receber `ComunicadoEncerramento`

### 3. OrdenacaoSequencial - Programa de Comparação
- Ordena o mesmo tamanho de vetor sem paralelismo/distribuição
- Usado para comparar tempos e validar a eficiência do sistema distribuído

## 📦 Classes Principais

### Hierarquia de Comunicação

```
Comunicado (Serializable)
├── Pedido
├── Resposta
└── ComunicadoEncerramento
```

### Comunicado
- Classe base que implementa `Serializable`
- Não possui atributos nem métodos
- Serve como superclasse para todos os tipos de comunicação

### Pedido
- **Atributos:**
  - `byte[] numeros` - Parte do vetor a ser ordenada
- **Métodos:**
  - `ordenar()` - Ordena o vetor usando Merge Sort recursivo
  - `mergeSort()` - Implementação recursiva do algoritmo
  - `merge()` - Intercala dois vetores ordenados

### Resposta
- **Atributos:**
  - `byte[] vetorOrdenado` - Vetor já ordenado
- **Métodos:**
  - `getVetor()` - Retorna o vetor ordenado

### ComunicadoEncerramento
- Sinal de término de comunicação
- Indica ao Receptor para fechar a conexão

## 🚀 Comandos Essenciais

> Os scripts foram removidos. Utilize os comandos abaixo diretamente no terminal.

### 1. Compilar todos os módulos

```bash
javac src/Comunicacao/*.java
javac -cp . src/Receptor/*.java
javac -cp . src/Distribuidor/*.java
javac -cp . src/OrdenacaoSequencial.java
javac -cp . src/MaiorVetorAproximado.java
```

### 2. Configurar IPs/portas dos receptores

Edite `src/Distribuidor/Distribuidor.java`, ajustando o vetor `servidores`:

```java
String[] servidores = {
    "192.168.0.10:12345",
    "192.168.0.11:12346"
};
```

Para testes na mesma máquina:

```java
String[] servidores = {
    "127.0.0.1:12345",
    "127.0.0.1:12346"
};
```

### 3. Executar os receptores (um terminal para cada instância)

```bash
java -cp . src.Receptor.Receptor 12345
java -cp . src.Receptor.Receptor 12346
```

### 4. Executar o distribuidor

```bash
java -Xmx2G -cp . src.Distribuidor.Distribuidor
```

### 5. Executar a ordenação sequencial (comparação de tempos)

```bash
java -Xmx2G -cp . src.OrdenacaoSequencial
```

### 6. (Opcional) Descobrir o maior vetor suportado pela máquina

```bash
java -Xmx4G -cp . src.MaiorVetorAproximado
```

## 🔧 Características Técnicas

### Paralelismo no Receptor

1. **Threads Ordenadoras:**
   - Quantidade: número de processadores disponíveis
   - Função: cada thread ordena uma parte do vetor usando Merge Sort
   - Controle: **manual** (sem thread pool)

2. **Threads Juntadoras:**
   - Fazem merge dos resultados 2 a 2 em rodadas
   - Quantidade máxima: número de processadores
   - Continuam até restar apenas um vetor ordenado

### Paralelismo no Distribuidor

1. **Threads de Comunicação:**
   - Uma thread por Receptor
   - Enviam pedidos e recebem respostas em paralelo

2. **Threads Juntadoras:**
   - Fazem merge dos vetores recebidos dos Receptores
   - Processamento 2 a 2 em múltiplas rodadas
   - Quantidade máxima: número de processadores

### Algoritmo Merge Sort

**Divisão e Conquista:**
1. Divide o vetor ao meio recursivamente
2. Ordena cada metade
3. Intercala (merge) as metades ordenadas

**Complexidade:**
- Tempo: O(n log n) - ótimo para ordenação baseada em comparação
- Espaço: O(n) - cria vetores temporários

### Comunicação

- **Protocolo:** TCP/IP
- **Formato:** Serialização de objetos Java
- **Conexões:** Persistentes (mantidas abertas)
- **Encerramento:** Via `ComunicadoEncerramento`

## ✅ Validação

O sistema valida automaticamente a ordenação:
- Percorre o vetor verificando se `vetor[i] <= vetor[i+1]`
- Exibe mensagem de `[SUCESSO]` ou `[ERRO]`

## 📊 Comparação de Desempenho

Execute ambos os programas com o mesmo tamanho:

| Tamanho   | Sequencial | Distribuído (2 máquinas) | Speedup |
|-----------|-----------|--------------------------|---------|
| 10.000    | ~50ms     | ~30ms                    | 1.7x    |
| 100.000   | ~500ms    | ~200ms                   | 2.5x    |
| 1.000.000 | ~5s       | ~1.5s                    | 3.3x    |

*Valores aproximados, variam conforme hardware*

## 🗂️ Estrutura de Arquivos

```
sistema-distribuidor-receptor-2/
├── src/
│   ├── Comunicacao/
│   │   ├── Comunicado.java
│   │   ├── Pedido.java
│   │   ├── Resposta.java
│   │   └── ComunicadoEncerramento.java
│   ├── Receptor/
│   │   └── Receptor.java
│   ├── Distribuidor/
│   │   └── Distribuidor.java
│   ├── OrdenacaoSequencial.java
│   └── MaiorVetorAproximado.java
├── README.md
└── INSTRUCOES.md
```

## 📝 Logs do Sistema

Os programas geram logs informativos:
- `[LOG]` - Informações normais
- `[ERRO]` - Erros capturados
- `[AVISO]` - Avisos importantes
- `[RESULTADO]` - Resultados finais
- `[SUCESSO]` - Validação bem-sucedida

**Exemplo de Log do Receptor:**
```
[LOG] Conexão #1 aceita de: 192.168.0.15:54321
[LOG] Conexão #1 - Pedido #1 recebido (tamanho vetor: 500000)
    [LOG] Iniciando ordenação com 8 threads
    [LOG] Thread ordenadora 0 iniciada para índices [0, 62500)
    [LOG] Thread ordenadora 1 iniciada para índices [62500, 125000)
    ...
    [LOG] Rodada de merge #1 - 8 vetores
    [LOG] Thread juntadora 0 iniciada
    ...
[LOG] Conexão #1 - Pedido #1 processado e respondido em 1250 ms
```

## 🛠️ Utilitários

### Descobrir Maior Vetor Possível

```bash
javac src/MaiorVetorAproximado.java
java -Xmx8G src.MaiorVetorAproximado
```

Descobre o maior vetor de bytes que pode ser alocado na memória disponível.

## ⚠️ Solução de Problemas

### "Connection refused"
- ✓ Verifique se os Receptores estão rodando
- ✓ Confirme os IPs e portas no Distribuidor
- ✓ Verifique firewall

### OutOfMemoryError
- ✓ Aumente memória: `java -Xmx4G ...`
- ✓ Reduza o tamanho do vetor
- ✓ Use mais Receptores para distribuir melhor

### Vetor não está ordenado
- ✓ Veja logs para identificar erros
- ✓ Teste com vetor menor primeiro
- ✓ Verifique se todas as threads completaram

## 📚 Conceitos Aprendidos

1. **Programação Distribuída:**
   - Comunicação TCP/IP
   - Serialização de objetos
   - Coordenação de múltiplas máquinas

2. **Programação Paralela:**
   - Controle manual de threads
   - Sincronização com `join()`
   - Divisão de trabalho entre processadores

3. **Algoritmos:**
   - Merge Sort (divisão e conquista)
   - Intercalação de vetores ordenados
   - Análise de complexidade

4. **Engenharia de Software:**
   - Tratamento de exceções
   - Logs informativos
   - Validação de resultados

## 👥 Autores

*[Nome dos membros do grupo]*

## 📅 Data

Novembro de 2025

## 📖 Referências

- Atividade #2 - Programação Paralela e Distribuída
- Algoritmos de Ordenação - Merge Sort
- Java Networking - Socket Programming
- Java Object Serialization

---

**Desenvolvido como trabalho acadêmico de Programação Paralela e Distribuída**
