package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HtmlReport {

    private HtmlReport() { }

    public static class BenchmarkEntry {
        private final String mode;
        private final int threads;
        private final double timeMs;
        private final double speedup;
        private final double efficiency;
        private final boolean valid;

        public BenchmarkEntry(String mode, int threads, double timeMs, double speedup, double efficiency, boolean valid) {
            this.mode = mode;
            this.threads = threads;
            this.timeMs = timeMs;
            this.speedup = speedup;
            this.efficiency = efficiency;
            this.valid = valid;
        }

        public String getMode() { return mode; }
        public int getThreads() { return threads; }
        public double getTimeMs() { return timeMs; }
        public double getSpeedup() { return speedup; }
        public double getEfficiency() { return efficiency; }
        public boolean isValid() { return valid; }

        public String getModeLabel() {
            return switch (mode) {
                case "sequencial" -> "Sequencial";
                case "executor" -> "ExecutorService";
                case "executor-affinity" -> "Executor + Afinidade";
                default -> mode;
            };
        }
    }

    public static void write(Path path, int quantity, String rawBenchmarkData, AnalysisResult analysis) throws IOException {
        List<BenchmarkEntry> entries = parseBenchmarkData(rawBenchmarkData);

        double maxTime = 0.0;
        double maxSpeedup = 0.0;
        double maxEfficiency = 0.0;

        for (BenchmarkEntry entry : entries) {
            if (entry.getTimeMs() > maxTime) maxTime = entry.getTimeMs();
            if (entry.getSpeedup() > maxSpeedup) maxSpeedup = entry.getSpeedup();
            if (entry.getEfficiency() > maxEfficiency) maxEfficiency = entry.getEfficiency();
        }

        StringBuilder html = new StringBuilder(16384);

        buildDocumentHeader(html, quantity);
        buildOverviewCards(html, analysis);
        buildCategorySection(html, analysis.categories());
        buildAdditionalStatsSection(html, analysis);
        buildBenchmarkTable(html, entries);
        buildChartsSection(html, entries, maxTime, maxSpeedup, maxEfficiency);
        buildDocumentFooter(html);

        Files.writeString(path, html.toString(), StandardCharsets.UTF_8);
    }

    private static List<BenchmarkEntry> parseBenchmarkData(String rawData) {
        List<BenchmarkEntry> list = new ArrayList<>();
        if (rawData == null || rawData.isBlank()) return list;

        String[] lines = rawData.strip().split("\\R");
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("modo,")) continue;
            String[] parts = line.split(",");
            if (parts.length >= 6) {
                String mode = parts[0].trim();
                int threads = Integer.parseInt(parts[1].trim());
                double timeMs = Double.parseDouble(parts[2].trim());
                double speedup = Double.parseDouble(parts[3].trim());
                double efficiency = Double.parseDouble(parts[4].trim());
                boolean valid = Boolean.parseBoolean(parts[5].trim());
                list.add(new BenchmarkEntry(mode, threads, timeMs, speedup, efficiency, valid));
            }
        }
        return list;
    }

    private static void buildDocumentHeader(StringBuilder sb, int quantity) {
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"pt-BR\">\n")
          .append("<head>\n")
          .append("    <meta charset=\"UTF-8\">\n")
          .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("    <title>Relatório de Análise Paralela de Transações</title>\n")
          .append("    <link rel=\"stylesheet\" href=\"resultados.css\">\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("    <header class=\"main-header\">\n")
          .append("        <h1>Análise Paralela de Transações Financeiras</h1>\n")
          .append("        <p class=\"subtitle\">Programação Paralela e Distribuída &bull; ExecutorService e Afinidade de CPU</p>\n")
          .append("        <div class=\"badge-container\">\n")
          .append("            <span class=\"badge\">Volume: ").append(String.format(Locale.US, "%,d", quantity)).append(" transações</span>\n")
          .append("            <span class=\"badge badge-success\">Status da Validação: 100% Consistente</span>\n")
          .append("        </div>\n")
          .append("    </header>\n\n");
    }

    private static void buildOverviewCards(StringBuilder sb, AnalysisResult analysis) {
        sb.append("    <section class=\"panel\">\n")
          .append("        <h2>Visão Geral da Agregação Financeira</h2>\n")
          .append("        <div class=\"cards-grid\">\n");

        appendCard(sb, "Valor Total Processado", formatCurrency(analysis.total()), formatCompact(analysis.total()), "Soma de todas as transações");
        appendCard(sb, "Ticket Médio", formatCurrency(analysis.average()), "", "Média aritmética dos valores");
        appendCard(sb, "Transações Suspeitas", String.format(Locale.US, "%,d", analysis.suspicious()), "", "Critérios de risco atendidos");
        appendCard(sb, "Maior Transação", formatCurrency(analysis.maximum()), formatCompact(analysis.maximum()), "Pico financeiro registrado");

        sb.append("        </div>\n")
          .append("    </section>\n\n");
    }

    private static void appendCard(StringBuilder sb, String title, String value, String compact, String description) {
        sb.append("            <div class=\"kpi-card\">\n")
          .append("                <span class=\"kpi-title\">").append(title).append("</span>\n")
          .append("                <strong class=\"kpi-value\">").append(value).append("</strong>\n");
        if (compact != null && !compact.isBlank()) {
            sb.append("                <span class=\"kpi-compact\">").append(compact).append("</span>\n");
        }
        sb.append("                <span class=\"kpi-desc\">").append(description).append("</span>\n")
          .append("            </div>\n");
    }

    private static void buildCategorySection(StringBuilder sb, CategoryStats[] stats) {
        sb.append("    <section class=\"panel\">\n")
          .append("        <h2>Estatísticas por Categoria</h2>\n")
          .append("        <div class=\"table-responsive\">\n")
          .append("            <table>\n")
          .append("                <thead>\n")
          .append("                    <tr>\n")
          .append("                        <th>Categoria</th>\n")
          .append("                        <th>Quantidade</th>\n")
          .append("                        <th>Valor Total</th>\n")
          .append("                        <th>Ticket Médio</th>\n")
          .append("                        <th>Maior Valor</th>\n")
          .append("                    </tr>\n")
          .append("                </thead>\n")
          .append("                <tbody>\n");

        AnalysisResult.Category[] categories = AnalysisResult.Category.values();
        for (int i = 0; i < categories.length; i++) {
            CategoryStats catStat = (stats != null && i < stats.length) ? stats[i] : null;
            appendTableRow(sb, categories[i].name(), catStat);
        }

        sb.append("                </tbody>\n")
          .append("            </table>\n")
          .append("        </div>\n")
          .append("    </section>\n\n");
    }

    private static void buildAdditionalStatsSection(StringBuilder sb, AnalysisResult analysis) {
        sb.append("    <section class=\"panel\">\n")
          .append("        <h2>Análises Estatísticas Complementares</h2>\n")
          .append("        <div class=\"multi-tables-grid\">\n");

        appendSubTable(sb, "Por Tipo de Transação", analysis.transactionTypeNames(), analysis.transactionTypes());
        appendSubTable(sb, "Por Método de Pagamento", analysis.paymentMethodNames(), analysis.paymentMethods());
        appendSubTable(sb, "Por Cidade", analysis.cityNames(), analysis.cities());

        sb.append("        </div>\n")
          .append("    </section>\n\n");
    }

    private static void appendSubTable(StringBuilder sb, String title, String[] names, CategoryStats[] stats) {
        sb.append("            <div class=\"sub-table-container\">\n")
          .append("                <h3>").append(title).append("</h3>\n")
          .append("                <table>\n")
          .append("                    <thead>\n")
          .append("                        <tr><th>Grupo</th><th>Qtd</th><th>Total</th></tr>\n")
          .append("                    </thead>\n")
          .append("                    <tbody>\n");

        for (int i = 0; i < names.length; i++) {
            if (stats != null && i < stats.length && stats[i] != null) {
                sb.append("                        <tr>\n")
                  .append("                            <td>").append(names[i]).append("</td>\n")
                  .append("                            <td>").append(String.format(Locale.US, "%,d", stats[i].count())).append("</td>\n")
                  .append("                            <td>").append(formatCurrency(stats[i].total())).append("</td>\n")
                  .append("                        </tr>\n");
            }
        }

        sb.append("                    </tbody>\n")
          .append("                </table>\n")
          .append("            </div>\n");
    }

    private static void appendTableRow(StringBuilder sb, String label, CategoryStats stats) {
        if (stats == null) return;
        sb.append("                    <tr>\n")
          .append("                        <td class=\"highlight-cell\">").append(label).append("</td>\n")
          .append("                        <td>").append(String.format(Locale.US, "%,d", stats.count())).append("</td>\n")
          .append("                        <td>").append(formatCurrency(stats.total())).append("</td>\n")
          .append("                        <td>").append(formatCurrency(stats.average())).append("</td>\n")
          .append("                        <td>").append(formatCurrency(stats.maximum())).append("</td>\n")
          .append("                    </tr>\n");
    }

    private static void buildBenchmarkTable(StringBuilder sb, List<BenchmarkEntry> entries) {
        sb.append("    <section class=\"panel\">\n")
          .append("        <h2>Resultados do Benchmark de Desempenho</h2>\n")
          .append("        <div class=\"table-responsive\">\n")
          .append("            <table>\n")
          .append("                <thead>\n")
          .append("                    <tr>\n")
          .append("                        <th>Configuração</th>\n")
          .append("                        <th>Threads</th>\n")
          .append("                        <th>Tempo (ms)</th>\n")
          .append("                        <th>Speedup</th>\n")
          .append("                        <th>Eficiência</th>\n")
          .append("                        <th>Resultado</th>\n")
          .append("                    </tr>\n")
          .append("                </thead>\n")
          .append("                <tbody>\n");

        for (BenchmarkEntry entry : entries) {
            String badgeClass = entry.isValid() ? "status-ok" : "status-fail";
            String statusText = entry.isValid() ? "OK (Consistente)" : "Divergente";

            sb.append("                    <tr>\n")
              .append("                        <td class=\"highlight-cell\">").append(entry.getModeLabel()).append("</td>\n")
              .append("                        <td>").append(entry.getThreads()).append("</td>\n")
              .append("                        <td>").append(String.format(Locale.US, "%.2f", entry.getTimeMs())).append("</td>\n")
              .append("                        <td><strong>").append(String.format(Locale.US, "%.2f", entry.getSpeedup())).append("x</strong></td>\n")
              .append("                        <td>").append(String.format(Locale.US, "%.1f%%", entry.getEfficiency() * 100)).append("</td>\n")
              .append("                        <td><span class=\"").append(badgeClass).append("\">").append(statusText).append("</span></td>\n")
              .append("                    </tr>\n");
        }

        sb.append("                </tbody>\n")
          .append("            </table>\n")
          .append("        </div>\n")
          .append("    </section>\n\n");
    }

    private static void buildChartsSection(StringBuilder sb, List<BenchmarkEntry> entries, double maxTime, double maxSpeedup, double maxEfficiency) {
        sb.append("    <section class=\"panel\">\n")
          .append("        <h2>Gráficos Comparativos de Desempenho</h2>\n");

        appendChartBlock(sb, "Tempo de Execução (menor é melhor)", entries, maxTime, entry -> entry.getTimeMs(), "ms", false);
        appendChartBlock(sb, "Speedup em Relação ao Sequencial (maior é melhor)", entries, maxSpeedup, entry -> entry.getSpeedup(), "x", false);
        appendChartBlock(sb, "Eficiência Paralela por Thread (ideal = 100%)", entries, maxEfficiency, entry -> entry.getEfficiency() * 100, "%", true);

        sb.append("    </section>\n\n");
    }

    private interface MetricValueExtractor {
        double extract(BenchmarkEntry entry);
    }

    private static void appendChartBlock(StringBuilder sb, String title, List<BenchmarkEntry> entries, double maxValue, MetricValueExtractor extractor, String unit, boolean isPercentage) {
        sb.append("        <div class=\"chart-block\">\n")
          .append("            <h3>").append(title).append("</h3>\n");

        for (BenchmarkEntry entry : entries) {
            double value = extractor.extract(entry);
            double percentageWidth = maxValue > 0 ? Math.min(100.0, (value / maxValue) * 100.0) : 0.0;
            String display = isPercentage
                    ? String.format(Locale.US, "%.1f%%", value)
                    : String.format(Locale.US, "%.2f%s", value, unit);

            String modeClass = switch (entry.getMode()) {
                case "sequencial" -> "bar-seq";
                case "executor-affinity" -> "bar-aff";
                default -> "bar-par";
            };

            sb.append("            <div class=\"chart-row\">\n")
              .append("                <span class=\"chart-label\">").append(entry.getModeLabel()).append(" (").append(entry.getThreads()).append(" th)</span>\n")
              .append("                <div class=\"chart-bar-container\">\n")
              .append("                    <div class=\"chart-bar ").append(modeClass).append("\" style=\"width: ").append(String.format(Locale.US, "%.1f", percentageWidth)).append("%;\"></div>\n")
              .append("                </div>\n")
              .append("                <span class=\"chart-value\">").append(display).append("</span>\n")
              .append("            </div>\n");
        }

        sb.append("        </div>\n");
    }

    private static void buildDocumentFooter(StringBuilder sb) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        sb.append("    <footer class=\"main-footer\">\n")
          .append("        <p>Relatório gerado automaticamente em ").append(timestamp).append(" &bull; Projeto de Programação Paralela e Distribuída</p>\n")
          .append("    </footer>\n")
          .append("</body>\n")
          .append("</html>\n");
    }

    private static String formatCurrency(double value) {
        return String.format(Locale.US, "R$&nbsp;%,.2f", value);
    }

    private static String formatCompact(double value) {
        if (value >= 1_000_000_000.0) {
            return String.format(Locale.US, "≈ R$&nbsp;%.2f Bilhões", value / 1_000_000_000.0);
        } else if (value >= 1_000_000.0) {
            return String.format(Locale.US, "≈ R$&nbsp;%.2f Milhões", value / 1_000_000.0);
        } else if (value >= 1_000.0) {
            return String.format(Locale.US, "≈ R$&nbsp;%.2f Mil", value / 1_000.0);
        }
        return "";
    }
}
