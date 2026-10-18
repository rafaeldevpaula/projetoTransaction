package app;

public final class AnalysisResult {
    private long count;
    private double total;
    private double maximum = Double.NEGATIVE_INFINITY;
    private double minimum = Double.POSITIVE_INFINITY;
    private long suspicious;
    public enum Category { ALIMENTACAO, TRANSPORTE, ELETRONICOS, SAUDE, EDUCACAO }
    private final CategoryStats[] categories = new CategoryStats[Category.values().length];
    private final String[] transactionTypeNames = {"DEBITO", "CREDITO"};
    private final CategoryStats[] transactionTypes = new CategoryStats[transactionTypeNames.length];
    private final String[] paymentMethodNames = {"CARTAO", "BOLETO", "DINHEIRO", "CARTEIRA_DIGITAL"};
    private final CategoryStats[] paymentMethods = new CategoryStats[paymentMethodNames.length];
    private final String[] cityNames = {"Recife", "Olinda", "Jaboatao", "Paulista", "Caruaru"};
    private final CategoryStats[] cities = new CategoryStats[cityNames.length];

    public void accept(Transaction transaction) {
        count++;
        total += transaction.value();
        maximum = Math.max(maximum, transaction.value());
        minimum = Math.min(minimum, transaction.value());
        if (transaction.value() > 10000 || (transaction.value() > 5000 && transaction.category().equals("Eletronicos"))) suspicious++;
        int categoryIndex = Category.valueOf(transaction.category().toUpperCase()).ordinal();
        add(categories, categoryIndex, transaction);
        add(transactionTypes, find(transactionTypeNames, transaction.type()), transaction);
        add(paymentMethods, find(paymentMethodNames, transaction.metodoPagamento()), transaction);
        add(cities, find(cityNames, transaction.city()), transaction);
    }

    public void merge(AnalysisResult other) {
        count += other.count;
        total += other.total;
        maximum = Math.max(maximum, other.maximum);
        minimum = Math.min(minimum, other.minimum);
        suspicious += other.suspicious;
        merge(categories, other.categories);
        merge(transactionTypes, other.transactionTypes);
        merge(paymentMethods, other.paymentMethods);
        merge(cities, other.cities);
    }

    public long count() { return count; }
    public double total() { return total; }
    public double average() { return count == 0 ? 0 : total / count; }
    public double maximum() { return count == 0 ? 0 : maximum; }
    public double minimum() { return count == 0 ? 0 : minimum; }
    public long suspicious() { return suspicious; }
    public CategoryStats[] categories() { return categories; }
    public CategoryStats[] transactionTypes() { return transactionTypes; }
    public String[] transactionTypeNames() { return transactionTypeNames; }
    public CategoryStats[] paymentMethods() { return paymentMethods; }
    public String[] paymentMethodNames() { return paymentMethodNames; }
    public CategoryStats[] cities() { return cities; }
    public String[] cityNames() { return cityNames; }

    private static void add(CategoryStats[] stats, int index, Transaction transaction) {
        if (stats[index] == null) stats[index] = new CategoryStats();
        stats[index].add(transaction);
    }

    private static void merge(CategoryStats[] target, CategoryStats[] source) {
        for (int index = 0; index < target.length; index++) {
            if (source[index] == null) continue;
            if (target[index] == null) target[index] = new CategoryStats();
            target[index].merge(source[index]);
        }
    }

    private static int find(String[] names, String value) {
        for (int index = 0; index < names.length; index++) {
            if (names[index].equals(value)) return index;
        }
        return 0;
    }
}