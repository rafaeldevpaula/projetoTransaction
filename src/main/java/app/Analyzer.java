package app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Analyzer {

    private Analyzer() { }

    public static class TarefaAnalise implements Callable<AnalysisResult> {
        private final Transaction[] transacoes;
        private final int inicio;
        private final int fim;
        private final int cpuAlvo;
        private final boolean aplicarAfinidade;

        public TarefaAnalise(Transaction[] transacoes, int inicio, int fim, int cpuAlvo, boolean aplicarAfinidade) {
            this.transacoes = transacoes;
            this.inicio = inicio;
            this.fim = fim;
            this.cpuAlvo = cpuAlvo;
            this.aplicarAfinidade = aplicarAfinidade;
        }

        @Override
        public AnalysisResult call() {
            if (aplicarAfinidade) {
                AffinitySupport.bind(cpuAlvo);
            }
            return analyzeRange(transacoes, inicio, fim);
        }
    }

    public static AnalysisResult sequential(Transaction[] transactions) {
        return analyzeRange(transactions, 0, transactions.length);
    }

    public static AnalysisResult parallel(Transaction[] transactions, int threads, boolean affinity) throws Exception {
        if (transactions == null || transactions.length == 0) {
            return new AnalysisResult();
        }

        int taskCount = Math.max(1, Math.min(threads, transactions.length));
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);

        try {
            int blockSize = (transactions.length + taskCount - 1) / taskCount;
            List<Future<AnalysisResult>> futures = new ArrayList<>(taskCount);

            for (int task = 0; task < taskCount; task++) {
                int start = task * blockSize;
                int end = Math.min(transactions.length, start + blockSize);
                int cpu = task;

                futures.add(executor.submit(new TarefaAnalise(transactions, start, end, cpu, affinity)));
            }

            AnalysisResult result = new AnalysisResult();
            for (Future<AnalysisResult> future : futures) {
                result.merge(future.get());
            }

            return result;
        } finally {
            executor.shutdown();
        }
    }

    private static AnalysisResult analyzeRange(Transaction[] transactions, int start, int end) {
        AnalysisResult result = new AnalysisResult();
        for (int index = start; index < end; index++) {
            result.accept(transactions[index]);
        }
        return result;
    }
}