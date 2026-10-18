package app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstatisticasPorCategoria {

    public static final String[] CATEGORIAS_PADRAO = {
        "Alimentacao", "Transporte", "Eletronicos", "Saude", "Educacao"
    };

    public static Map<String, CategoryStats> criarMapaInicial() {
        Map<String, CategoryStats> mapa = new LinkedHashMap<>();
        for (String cat : CATEGORIAS_PADRAO) {
            mapa.put(cat, new CategoryStats());
        }
        return mapa;
    }

    public static void mesclarMapas(Map<String, CategoryStats> destino, Map<String, CategoryStats> origem) {
        if (origem == null) {
            return;
        }
        for (Map.Entry<String, CategoryStats> entry : origem.entrySet()) {
            String categoria = entry.getKey();
            CategoryStats statOrigem = entry.getValue();

            CategoryStats statDestino = destino.computeIfAbsent(categoria, k -> new CategoryStats());
            statDestino.merge(statOrigem);
        }
    }

    public static class TarefaCategoria implements Callable<Map<String, CategoryStats>> {
        private final Transaction[] transacoes;
        private final int inicio;
        private final int fim;
        private final int cpuAlvo;
        private final boolean aplicarAfinidade;

        public TarefaCategoria(Transaction[] transacoes, int inicio, int fim, int cpuAlvo, boolean aplicarAfinidade) {
            this.transacoes = transacoes;
            this.inicio = inicio;
            this.fim = fim;
            this.cpuAlvo = cpuAlvo;
            this.aplicarAfinidade = aplicarAfinidade;
        }

        @Override
        public Map<String, CategoryStats> call() {
            if (aplicarAfinidade) {
                AffinitySupport.bind(cpuAlvo);
            }

            Map<String, CategoryStats> mapaLocal = criarMapaInicial();
            for (int i = inicio; i < fim; i++) {
                Transaction t = transacoes[i];
                CategoryStats stat = mapaLocal.get(t.getCategory());
                if (stat != null) {
                    stat.add(t);
                } else {
                    CategoryStats novo = new CategoryStats();
                    novo.add(t);
                    mapaLocal.put(t.getCategory(), novo);
                }
            }
            return mapaLocal;
        }
    }

    public static Map<String, CategoryStats> executarSequencial(Transaction[] transacoes) {
        Map<String, CategoryStats> resultado = criarMapaInicial();
        for (int i = 0; i < transacoes.length; i++) {
            Transaction t = transacoes[i];
            CategoryStats stat = resultado.get(t.getCategory());
            if (stat != null) {
                stat.add(t);
            } else {
                CategoryStats novo = new CategoryStats();
                novo.add(t);
                resultado.put(t.getCategory(), novo);
            }
        }
        return resultado;
    }

    public static Map<String, CategoryStats> executarParalelo(Transaction[] transacoes, int numeroThreads, boolean comAfinidade) throws Exception {
        if (transacoes == null || transacoes.length == 0) {
            return criarMapaInicial();
        }

        int quantidadeTarefas = Math.max(1, Math.min(numeroThreads, transacoes.length));
        ExecutorService executor = Executors.newFixedThreadPool(quantidadeTarefas);

        try {
            int tamanhoBloco = (transacoes.length + quantidadeTarefas - 1) / quantidadeTarefas;
            List<Future<Map<String, CategoryStats>>> futuros = new ArrayList<>();

            for (int tarefa = 0; tarefa < quantidadeTarefas; tarefa++) {
                int inicio = tarefa * tamanhoBloco;
                int fim = Math.min(transacoes.length, inicio + tamanhoBloco);
                int cpu = tarefa;

                futuros.add(executor.submit(new TarefaCategoria(transacoes, inicio, fim, cpu, comAfinidade)));
            }

            Map<String, CategoryStats> resultadoFinal = criarMapaInicial();
            for (Future<Map<String, CategoryStats>> futuro : futuros) {
                mesclarMapas(resultadoFinal, futuro.get());
            }

            return resultadoFinal;
        } finally {
            executor.shutdown();
        }
    }
}
