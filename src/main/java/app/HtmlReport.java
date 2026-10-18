package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class HtmlReport {
    private HtmlReport() { }

    public static void write(Path path, int quantity, String rows, AnalysisResult analysis) throws Exception {
        String[] records = rows.strip().split("\\R");
        double[] maximums = maximums(records);
        String html = "<!doctype html>";
        html += "<html lang='pt-BR'><head><meta charset='UTF-8'>";
        html += "<title>Resultados da analise</title>";
        html += "<link rel='stylesheet' href='resultados.css'></head><body>";
        html += "<h1>Analise paralela de transacoes financeiras</h1>";
        html += "<p class='subtitle'>Comparacao visual entre os modos</p>";
        html += "<p><small>Quantidade: " + quantity + "</small></p>";

        html += "<section class='panel'><h2>Analises financeiras</h2><div class='cards'>";
        html += card("Valor total", money(analysis.total()));
        html += card("Valor medio", money(analysis.average()));
        html += card("Suspeitas", String.format(Locale.US, "%,d", analysis.suspicious()));
        html += card("Maior transacao", money(analysis.maximum()));
        html += "</div></section>";

        html += categoryTable(analysis.categories());
        html += statsTable("Analise por tipo", analysis.transactionTypeNames(), analysis.transactionTypes());
        html += statsTable("Analise por pagamento", analysis.paymentMethodNames(), analysis.paymentMethods());
        html += statsTable("Analise por cidade", analysis.cityNames(), analysis.cities());
        html += chart("Tempo (ms)", records, 2, maximums[0]);
        html += chart("Speedup", records, 3, maximums[1]);
        html += chart("Eficiencia", records, 4, maximums[2]);
        html += "</body></html>";

        Files.writeString(path, html);
    }

    private static double[] maximums(String[] records) {
        double[] result = {0, 0, 0};
        for (String record : records) {
            String[] values = record.split(",");
            for (int metric = 0; metric < result.length; metric++) {
                double currentValue = Double.parseDouble(values[metric + 2]);
                result[metric] = Math.max(result[metric], currentValue);
            }
        }
        return result;
    }

    private static String categoryTable(CategoryStats[] stats) {
        String html = "<section class='panel'><h2>Estatisticas por categoria</h2>";
        html += "<table><tr><th>Categoria</th><th>Quantidade</th><th>Total</th><th>Media</th><th>Maior</th></tr>";
        AnalysisResult.Category[] categories = AnalysisResult.Category.values();

        for (int index = 0; index < categories.length; index++) {
            html += row(categories[index].toString(), stats[index]);
        }
        return html + "</table></section>";
    }

    private static String statsTable(String title, String[] names, CategoryStats[] stats) {
        String html = "<section class='panel'><h2>" + title + "</h2>";
        html += "<table><tr><th>Grupo</th><th>Quantidade</th><th>Total</th><th>Media</th><th>Maior</th></tr>";

        for (int index = 0; index < names.length; index++) {
            if (stats[index] != null) html += row(names[index], stats[index]);
        }
        return html + "</table></section>";
    }

    private static String row(String name, CategoryStats stats) {
        if (stats == null) return "";
        String html = "<tr><td class='category-total'>" + name + "</td>";
        html += "<td>" + String.format(Locale.US, "%,d", stats.count()) + "</td>";
        html += "<td>" + money(stats.total()) + "</td>";
        html += "<td>" + money(stats.average()) + "</td>";
        html += "<td>" + money(stats.maximum()) + "</td></tr>";
        return html;
    }

    private static String chart(String title, String[] records, int index, double maximum) {
        String html = "<section class='panel'><h2>" + title + "</h2>";
        for (String record : records) {
            String[] values = record.split(",");
            double value = Double.parseDouble(values[index]);
            String display = index == 4
                    ? String.format(Locale.US, "%.1f%%", value * 100)
                    : String.format(Locale.US, "%.2f", value);
            html += "<div class='chart'>";
            html += "<span class='label'>" + values[0] + " / " + values[1] + "</span>";
            html += "<progress value='" + value + "' max='" + maximum + "'></progress>";
            html += "<span class='number'>" + display + "</span></div>";
        }
        return html + "</section>";
    }

    private static String card(String label, String value) {
        return "<div class='card'><span>" + label + "</span><strong>" + value + "</strong></div>";
    }

    private static String money(double value) {
        return String.format(Locale.US, "R$ %,.2f", value);
    }
}
