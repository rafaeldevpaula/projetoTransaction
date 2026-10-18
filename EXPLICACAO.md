# Análise Paralela de Transações Financeiras

**Disciplina:** Programação Paralela e Distribuída (JCRPGPD)  
**Tema:** Análise Paralela utilizando `ExecutorService` e Afinidade de Processador  

---

## 1. Objetivo do Trabalho

Desenvolver uma aplicação em Java para análise de grandes volumes de transações financeiras utilizando a técnica de **paralelismo de dados**.

O sistema compara três abordagens:
1. **Sequencial (Baseline - 1 thread):** Processamento tradicional linear.
2. **Paralelo com ExecutorService:** Divisão do conjunto em fatias contíguas atribuídas a um pool de threads gerenciado.
3. **Paralelo com ExecutorService + Afinidade de CPU:** Associação das threads a processadores/núcleos específicos (CPU Pinning).

---

## 2. Divisão das Responsabilidades no Grupo

Conforme solicitado nas **Seções 8 e 15** do regulamento da avaliação, o projeto está estruturado em módulos individuais claramente atribuídos a cada integrante:

### 👤 Aluno 1 — Agregação Financeira (`AgregacaoFinanceira.java`)
- **Responsabilidades:**
  - Cálculo do valor total transacionado (soma);
  - Cálculo do valor médio das transações (ticket médio);
  - Identificação da maior transação registrada;
  - Identificação da menor transação registrada.
- **Implementações fornecidas:**
  - Versão sequencial;
  - Versão paralela com `ExecutorService` e tarefa explícita `Callable<Resultado>`;
  - Versão paralela com suporte a afinidade de CPU.

### 👤 Aluno 2 — Detecção de Transações Suspeitas (`DetectorTransacoesSuspeitas.java`)
- **Responsabilidades:**
  - Implementação das regras de negócio de detecção de fraudes/riscos:
    1. *Regra 1:* Transação com valor > R$ 10.000,00;
    2. *Regra 2:* Transação com valor > R$ 5.000,00 na categoria "Eletrônicos".
  - Contabilização da quantidade e do montante financeiro sob suspeita.
- **Implementações fornecidas:**
  - Versão sequencial e versão paralela com partição dos dados e redução.

### 👤 Aluno 3 — Estatísticas por Categoria (`EstatisticasPorCategoria.java` e `CategoryStats.java`)
- **Responsabilidades:**
  - Classificação e cálculo estatístico para as 5 categorias obrigatórias:
    - *Alimentação, Transporte, Eletrônicos, Saúde e Educação*.
  - Para cada categoria calcula: quantidade, valor total, valor médio e maior transação.
- **Implementações fornecidas:**
  - Versão sequencial e versão paralela com mapas de categorias e redução concorrente (`merge`).

### 👤 Aluno 4 — Benchmark e Análise de Desempenho (`Main.java` e `HtmlReport.java`)
- **Responsabilidades:**
  - Implementação do módulo de experimentação e medição precisa de tempos com `System.nanoTime()`;
  - Aquecimento prévio da JVM (Warm-up) para evitar distorções do compilador JIT;
  - Cálculo do **Speedup** ($S = \frac{T_{sequencial}}{T_{paralelo}}$);
  - Cálculo da **Eficiência Paralela** ($E = \frac{Speedup}{N_{threads}}$);
  - Exibição dos resultados no console no layout padronizado da Seção 18 do PDF;
  - Geração dos relatórios gráficos visuais (`resultados.html`) e dados tabulares (`.csv`).

---

## 3. Arquitetura e Paralelismo de Dados

- **Divisão dos Dados:** Para um conjunto de $N$ transações e $T$ threads, o vetor é particionado em blocos contíguos de tamanho balanceado $\lceil N / T \rceil$. Cada thread processa exclusivamente sua faixa $[inicio, fim)$.
- **Requisito Importante (Seção 13):** O programa **não** cria uma thread por transação; cria apenas um número de tarefas proporcional aos núcleos/threads configurados.
- **Redução de Resultados:** Cada tarefa retorna um resultado parcial (`Future<T>`). Ao final do processamento, a thread principal faz a consolidação (`merge`), garantindo a consistência matemática dos dados.

---

## 4. Como Compilar e Executar

### Opção A: Pelo VS Code
1. Abra a aba **Run and Debug** (`Ctrl + Shift + D`).
2. Selecione **Executar analise de transacoes** e clique no botão Play (verde).
3. A tarefa compilará os arquivos automaticamente para a pasta `out` e iniciará o benchmark.

### Opção B: Pelo Terminal PowerShell
```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 25 -d out (Get-ChildItem -Recurse src/main/java -Filter *.java).FullName
java -cp out app.Main --quantidade=100000 --threads=1,2,4,8
```

Após a execução, abra o arquivo `resultados.html` no navegador para visualizar o painel gráfico com tabelas e gráficos de barras.