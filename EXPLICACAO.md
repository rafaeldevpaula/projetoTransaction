# Explicacao do projeto

## 1. Objetivo

O projeto analisa transacoes financeiras sinteticas e compara tres formas de processamento: sequencial, paralelo com `ExecutorService` e paralelo com tentativa de afinidade de processador.

## 2. Estrutura dos arquivos

- `Transaction.java`: representa uma transacao usando um `record`.
- `TransactionGenerator.java`: cria dados sinteticos reproduziveis.
- `AnalysisResult.java`: calcula soma, media, maior valor, menor valor, suspeitas e estatisticas por categoria.
- `CategoryStats.java`: guarda quantidade, total, media e maior valor de uma categoria.
- `Analyzer.java`: contem as versoes sequencial e paralela da analise.
- `AffinitySupport.java`: tenta associar a tarefa a uma CPU quando a biblioteca OpenHFT esta disponivel.
- `Main.java`: recebe parametros, mede tempos, compara resultados e gera os arquivos de resultados.
- `.vscode/`: configura a compilacao e execucao no VS Code sem Maven.

## 3. Modelo da transacao

Cada objeto `Transaction` possui id, cliente, data, valor, categoria, cidade, estado, tipo, estabelecimento e metodo de pagamento. O vetor de transacoes e criado em memoria para que todas as abordagens processem exatamente os mesmos dados.

## 4. Geracao dos dados

O gerador usa uma semente fixa, garantindo que os testes sejam reproduziveis. Os valores alternam entre quatro distribuicoes: uniforme, normal, exponencial e concentrada. Algumas transacoes recebem valores altos para testar a regra de suspeita.

## 5. Analise sequencial

O metodo `Analyzer.sequential` percorre o vetor uma vez. Para cada transacao, `AnalysisResult.accept` atualiza soma, quantidade, extremos, suspeitas e o mapa da categoria. A media e calculada por `total / quantidade`.

Uma transacao e considerada suspeita quando possui valor acima de 10.000 ou quando possui valor acima de 5.000 e categoria `Eletronicos`.

Além das análises exigidas, o sistema calcula estatísticas por tipo de transação, por método de pagamento e por cidade. Para cada grupo são mostrados quantidade, valor total, valor médio e maior valor.

## 6. Paralelismo de dados

O vetor e dividido em blocos contiguos. Para quatro threads, por exemplo, cada tarefa recebe uma faixa diferente do vetor. O programa cria uma tarefa por bloco, e nao uma thread por transacao.

Cada tarefa executa o mesmo algoritmo em sua parte e retorna um `AnalysisResult`. Ao final, o programa combina os resultados parciais usando `merge`. Essa combinacao e a reducao dos resultados paralelos.

## 7. ExecutorService

`Analyzer.parallel` cria um `ExecutorService` com um numero fixo de threads. Cada bloco e enviado como `Callable<AnalysisResult>`, permitindo que a tarefa devolva seu resultado. A classe `Future` representa o resultado que sera obtido depois.

O executor e encerrado no bloco `finally`, garantindo que as threads sejam liberadas mesmo se ocorrer uma excecao.

## 8. Afinidade de processador

Quando o modo de afinidade esta ativo, cada tarefa recebe o indice da CPU correspondente. `AffinitySupport` procura a classe da biblioteca OpenHFT por reflexao. Se a biblioteca ou o suporte nativo nao estiverem disponiveis, a analise continua funcionando, mas a afinidade nao e aplicada.

## 9. Benchmark

`Main` mede o tempo com `System.nanoTime`. Para cada quantidade de threads, executa os modos `executor` e `executor-affinity`. Depois confere se a contagem e a soma coincidem com o resultado sequencial.

As formulas usadas sao:

```text
speedup = tempo_sequencial / tempo_paralelo
eficiencia = speedup / numero_de_threads
```

Um speedup maior indica que o paralelo foi mais rapido. A eficiencia mostra quanto do potencial das threads foi aproveitado.

## 10. Graficos

Ao final, `Main` cria `resultados.html`. O relatorio apresenta barras proporcionais para tempo, speedup e eficiencia, usando uma cor para cada modo. O arquivo pode ser aberto no navegador e usado para capturar imagens para o relatorio da disciplina.

## 11. Como executar

No VS Code, abra **Run and Debug** e selecione **Executar analise de transacoes**. A tarefa compila os arquivos para a pasta temporaria `out` e inicia `Main`.

No PowerShell:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 25 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp out app.Main --quantidade=100000 --threads=1,2,4,8
```

Depois, abra `resultados.html`.

## 12. Conclusao

O projeto demonstra paralelismo de dados porque varias tarefas executam o mesmo algoritmo sobre partes diferentes do vetor. A comparacao dos tempos permite discutir overhead, balanceamento, quantidade de nucleos, speedup, eficiencia e o efeito pratico da afinidade de processador.