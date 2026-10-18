# Análise paralela de transações financeiras

Projeto da avaliação de Programação Paralela e Distribuída. Ele compara a análise sequencial, o `ExecutorService` e o `ExecutorService` com tentativa de afinidade de CPU via OpenHFT.

## Execução

O projeto não precisa de Maven. Use apenas o Java instalado:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 25 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp out app.Main --quantidade=100000 --threads=1,2,4,8
```

Ao terminar, abra `resultados.html` no navegador. Ele contém gráficos de tempo, speedup e eficiência, além da tabela completa. Para também gerar CSV, acrescente `--csv=resultados.csv`.

O gerador usa quatro distribuições sintéticas: uniforme, normal, exponencial e concentrada por categoria. A análise principal usa apenas a biblioteca padrão do Java. A afinidade só é ativada quando o JAR OpenHFT for colocado no classpath; sem ele, o benchmark continua executando normalmente.

## Critérios implementados

- soma, média, maior e menor transação;
- detecção de suspeitas (`valor > 10000` ou eletrônicos acima de `5000`);
- estatísticas por categoria;
- divisão em blocos, com uma tarefa por bloco e nunca uma thread por transação;
- tempo, speedup e eficiência;
- relatório gráfico HTML com tempo, speedup e eficiência;
- saída CSV opcional com `--csv=resultados.csv`.

Para volumes grandes, prefira uma execução por vez e ajuste `--quantidade` à memória disponível.