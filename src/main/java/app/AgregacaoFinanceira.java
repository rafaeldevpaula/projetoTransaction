package app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AgregacaoFinanceira {

    public static class Resultado {
        private long quantidade;
        private double total;
        private double maiorValor = Double.NEGATIVE_INFINITY;
        private double menorValor = Double.POSITIVE_INFINITY;

        public void acumular(double valor) {
            this.quantidade++;
            this.total += valor;
            if (valor > this.maiorValor) {
                this.maiorValor = valor;
            }
            if (valor < this.menorValor) {
                this.menorValor = valor;
            }
        }

        public void mesclar(Resultado outro) {
            if (outro == null || outro.quantidade == 0) {
                return;
            }
            this.quantidade += outro.quantidade;
            this.total += outro.total;
            if (outro.maiorValor > this.maiorValor) {
                this.maiorValor = outro.maiorValor;
            }
            if (outro.menorValor < this.menorValor) {
                this.menorValor = outro.menorValor;
            }
        }

        public long getQuantidade() {
            return quantidade;
        }

        public double getTotal() {
            return total;
        }

        public double getMedia() {
            if (quantidade == 0) {
                return 0.0;
            }
            return total / quantidade;
        }

        public double getMaiorValor() {
            return quantidade == 0 ? 0.0 : maiorValor;
        }

        public double getMenorValor() {
            return quantidade == 0 ? 0.0 : menorValor;
        }
    }

    public static class TarefaAgregacao implements Callable<Resultado> {
        private final Transaction[] transacoes;
        private final int inicio;
        private final int fim;
        private final int cpuAlvo;
        private final boolean aplicarAfinidade;

        public TarefaAgregacao(Transaction[] transacoes, int inicio, int fim, int cpuAlvo, boolean aplicarAfinidade) {
            this.transacoes = transacoes;
            this.inicio = inicio;
            this.fim = fim;
            this.cpuAlvo = cpuAlvo;
            this.aplicarAfinidade = aplicarAfinidade;
        }

        @Override
        public Resultado call() {
            if (aplicarAfinidade) {
                AffinitySupport.bind(cpuAlvo);
            }

            Resultado resultadoParcial = new Resultado();
            for (int i = inicio; i < fim; i++) {
                resultadoParcial.acumular(transacoes[i].getValue());
            }
            return resultadoParcial;
        }
    }

    public static Resultado executarSequencial(Transaction[] transacoes) {
        Resultado resultado = new Resultado();
        for (int i = 0; i < transacoes.length; i++) {
            resultado.acumular(transacoes[i].getValue());
        }
        return resultado;
    }

    public static Resultado executarParalelo(Transaction[] transacoes, int numeroThreads, boolean comAfinidade) throws Exception {
        if (transacoes == null || transacoes.length == 0) {
            return new Resultado();
        }

        int quantidadeTarefas = Math.max(1, Math.min(numeroThreads, transacoes.length));
        ExecutorService executor = Executors.newFixedThreadPool(quantidadeTarefas);

        try {
            int tamanhoBloco = (transacoes.length + quantidadeTarefas - 1) / quantidadeTarefas;
            List<Future<Resultado>> futuros = new ArrayList<>();

            for (int tarefa = 0; tarefa < quantidadeTarefas; tarefa++) {
                int inicio = tarefa * tamanhoBloco;
                int fim = Math.min(transacoes.length, inicio + tamanhoBloco);
                int cpu = tarefa;

                futuros.add(executor.submit(new TarefaAgregacao(transacoes, inicio, fim, cpu, comAfinidade)));
            }

            Resultado resultadoFinal = new Resultado();
            for (Future<Resultado> futuro : futuros) {
                resultadoFinal.mesclar(futuro.get());
            }

            return resultadoFinal;
        } finally {
            executor.shutdown();
        }
    }
}
