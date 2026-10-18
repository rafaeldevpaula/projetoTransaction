package app;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        int quantity = optionInt(args, "quantidade", 100_000);
        int[] threads = Arrays.stream(option(args, "threads", "1,2,4").split(",")).mapToInt(Integer::parseInt).toArray();
        String csv = option(args, "csv", "");
        String html = option(args, "html", "resultados.html");
        Transaction[] transactions = TransactionGenerator.generate(quantity, 20261018L);
        long start = System.nanoTime();
        AnalysisResult expected = Analyzer.sequential(transactions);
        long sequentialNanos = System.nanoTime() - start;
        String rows = "sequencial,1," + sequentialNanos / 1_000_000.0 + ",1.0,1.0,true\n";
        for (int threadCount : threads) {
            rows += runBenchmark("executor", transactions, expected, threadCount, false, sequentialNanos);
            rows += runBenchmark("executor-affinity", transactions, expected, threadCount, true, sequentialNanos);
        }
        if (!csv.isBlank()) try (BufferedWriter writer = Files.newBufferedWriter(Path.of(csv))) { writer.write("modo,threads,tempo_ms,speedup,eficiencia,resultado_ok\n"); writer.write(rows); }
        HtmlReport.write(Path.of(html), quantity, rows, expected);
        System.out.println("Resultados salvos em: " + html);
    }

    private static String runBenchmark(String mode, Transaction[] data, AnalysisResult expected, int threads, boolean affinity, long sequentialNanos) throws Exception {
        long start = System.nanoTime();
        AnalysisResult actual = Analyzer.parallel(data, threads, affinity);
        long elapsed = System.nanoTime() - start;
        double speedup = (double) sequentialNanos / elapsed;
        double efficiency = speedup / threads;
        boolean ok = actual.count() == expected.count() && Math.abs(actual.total() - expected.total()) < 0.01;
        return mode + "," + threads + "," + (elapsed / 1_000_000.0) + "," + speedup + "," + efficiency + "," + ok + "\n";
    }

    private static String option(String[] args, String name, String fallback) { for (String arg : args) if (arg.startsWith("--" + name + "=")) return arg.substring(name.length() + 3); return fallback; }
    private static int optionInt(String[] args, String name, int fallback) { return Integer.parseInt(option(args, name, Integer.toString(fallback))); }

}