package app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DetectorTransacoesSuspeitas {

    public static class Resultado {
        private long quantidadeSuspeitas;
        private double valorTotalSuspeito;

        public void registrarSuspeita(double valor) {
            this.quantidadeSuspeitas++;
            this.valorTotalSuspeito += valor;
        }

        public void mesclar(Resultado outro) {
            if (outro == null) {
                return;
            }
            this.quantidadeSuspeitas += outro.quantidadeSuspeitas;
            this.valorTotalSuspeito += outro.valorTotalSuspeito;
        }

        public long getQuantidadeSuspeitas() {
            return quantidadeSuspeitas;
        }

        public double getValorTotalSuspeito() {
            return valorTotalSuspeito;
        }
    }

    public static boolean ehSuspeita(Transaction t) {
        if (t.getValue() > 10000.0) {
            return true;
        }
        if (t.getValue() > 5000.0 && "Eletronicos".equalsIgnoreCase(t.getCategory())) {
            return true;
        }
        return false;
    }

    public static class TarefaDetecao implements Callable<Resultado> {
        private final Transaction[] transacoes;
        private final int inicio;
        private final int fim;
        private final int cpuAlvo;
        private final boolean aplicarAfinidade;

        public TarefaDetecao(Transaction[] transacoes, int inicio, int fim, int cpuAlvo, boolean aplicarAfinidade) {
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
                Transaction t = transacoes[i];
                if (ehSuspeita(t)) {
                    resultadoParcial.registrarSuspeita(t.getValue());
                }
            }
            return resultadoParcial;
        }
    }

    public static Resultado executarSequencial(Transaction[] transacoes) {
        Resultado resultado = new Resultado();
        for (int i = 0; i < transacoes.length; i++) {
            Transaction t = transacoes[i];
            if (ehSuspeita(t)) {
                resultado.registrarSuspeita(t.getValue());
            }
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

                futuros.add(executor.submit(new TarefaDetecao(transacoes, inicio, fim, cpu, comAfinidade)));
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
