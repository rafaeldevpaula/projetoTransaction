package app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Locale;

public final class Main {

    private Main() { }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        try {
            int quantidade = lerParametroInteiro(args, "quantidade", 100_000);
            int[] threadsConfiguradas = lerParametroVetorInteiros(args, "threads", new int[]{1, 2, 4, 8});
            String caminhoCsv = lerParametroTexto(args, "csv", "");
            String caminhoHtml = lerParametroTexto(args, "html", "resultados.html");

            System.out.println("Gerando " + String.format(Locale.US, "%,d", quantidade) + " transações sintéticas...");
            Transaction[] transacoes = TransactionGenerator.generate(quantidade, 20261018L);

            for (int w = 0; w < 3; w++) {
                Analyzer.sequential(transacoes);
            }

            System.out.println("\n==============================================");
            System.out.println("ANÁLISE PARALELA DE TRANSAÇÕES FINANCEIRAS");
            System.out.println("==============================================");
            System.out.println("Quantidade de transações: " + String.format(Locale.US, "%,d", quantidade));

            System.out.println("---------------- SEQUENCIAL -----------------");
            long inicioSequencial = System.nanoTime();
            AnalysisResult resultadoEsperado = Analyzer.sequential(transacoes);
            long tempoSequencialNanos = System.nanoTime() - inicioSequencial;
            double tempoSequencialMs = tempoSequencialNanos / 1_000_000.0;

            System.out.println(String.format("Tempo: %.3f ms", tempoSequencialMs));

            StringBuilder csvBuffer = new StringBuilder();
            csvBuffer.append("sequencial,1,")
                     .append(tempoSequencialMs)
                     .append(",1.0,1.0,true\n");

            System.out.println("---------------- EXECUTOR -------------------");
            for (int numThreads : threadsConfiguradas) {
                RegistroBenchmark reg = executarTeste(
                    "executor", transacoes, resultadoEsperado, numThreads, false, tempoSequencialNanos
                );

                System.out.println("Threads: " + numThreads);
                System.out.println(String.format("Tempo: %.3f ms", reg.tempoMs));
                System.out.println(String.format("Speedup: %.2f", reg.speedup));
                System.out.println(String.format("Eficiência: %.1f%%", reg.eficiencia * 100));
                System.out.println();

                csvBuffer.append(reg.paraCsv()).append("\n");
            }

            System.out.println("----------- EXECUTOR + AFFINITY -------------");
            for (int numThreads : threadsConfiguradas) {
                RegistroBenchmark reg = executarTeste(
                    "executor-affinity", transacoes, resultadoEsperado, numThreads, true, tempoSequencialNanos
                );

                StringBuilder cpus = new StringBuilder();
                for (int c = 0; c < numThreads; c++) {
                    if (c > 0) {
                        cpus.append(",");
                    }
                    cpus.append(c);
                }

                System.out.println("Threads: " + numThreads);
                System.out.println("CPUs: " + cpus.toString());
                System.out.println(String.format("Tempo: %.3f ms", reg.tempoMs));
                System.out.println(String.format("Speedup: %.2f", reg.speedup));
                System.out.println(String.format("Eficiência: %.1f%%", reg.eficiencia * 100));
                System.out.println();

                csvBuffer.append(reg.paraCsv()).append("\n");
            }

            if (!caminhoCsv.isBlank()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(caminhoCsv))) {
                    writer.write("modo,threads,tempo_ms,speedup,eficiencia,resultado_ok\n");
                    writer.write(csvBuffer.toString());
                }
                System.out.println("Arquivo CSV gerado em: " + caminhoCsv);
            }

            HtmlReport.write(Path.of(caminhoHtml), quantidade, csvBuffer.toString(), resultadoEsperado);
            System.out.println("Relatório HTML visual salvo em: " + caminhoHtml);

        } catch (Exception e) {
            System.err.println("Erro durante a execução do benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class RegistroBenchmark {
        String modo;
        int threads;
        double tempoMs;
        double speedup;
        double eficiencia;
        boolean correto;

        String paraCsv() {
            return modo + "," + threads + "," + tempoMs + "," + speedup + "," + eficiencia + "," + correto;
        }
    }

    private static RegistroBenchmark executarTeste(String modo, Transaction[] dados, AnalysisResult esperado,
                                                   int threads, boolean afinidade, long tempoSeqNanos) throws Exception {
        long inicio = System.nanoTime();
        AnalysisResult obtido = Analyzer.parallel(dados, threads, afinidade);
        long decorridoNanos = System.nanoTime() - inicio;

        RegistroBenchmark reg = new RegistroBenchmark();
        reg.modo = modo;
        reg.threads = threads;
        reg.tempoMs = decorridoNanos / 1_000_000.0;
        reg.speedup = (double) tempoSeqNanos / decorridoNanos;
        reg.eficiencia = reg.speedup / threads;

        boolean contagemBate = (obtido.count() == esperado.count());
        boolean totalBate = Math.abs(obtido.total() - esperado.total()) < 0.01;
        reg.correto = contagemBate && totalBate;

        return reg;
    }

    private static String lerParametroTexto(String[] args, String nome, String valorPadrao) {
        String prefixo = "--" + nome + "=";
        for (String arg : args) {
            if (arg.startsWith(prefixo)) {
                return arg.substring(prefixo.length()).trim();
            }
        }
        return valorPadrao;
    }

    private static int lerParametroInteiro(String[] args, String nome, int valorPadrao) {
        String texto = lerParametroTexto(args, nome, "");
        if (texto.isBlank()) {
            return valorPadrao;
        }
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return valorPadrao;
        }
    }

    private static int[] lerParametroVetorInteiros(String[] args, String nome, int[] padrao) {
        String texto = lerParametroTexto(args, nome, "");
        if (texto.isBlank()) {
            return padrao;
        }
        try {
            String[] partes = texto.split(",");
            int[] valores = new int[partes.length];
            for (int i = 0; i < partes.length; i++) {
                valores[i] = Integer.parseInt(partes[i].trim());
            }
            return valores;
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}