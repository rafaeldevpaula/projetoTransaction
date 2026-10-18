package app;

public final class AnalysisResult {
    private long count;
    private double total;
    private double maximum = Double.NEGATIVE_INFINITY;
    private double minimum = Double.POSITIVE_INFINITY;
    private long suspicious;

    public enum Category {
        ALIMENTACAO,
        TRANSPORTE,
        ELETRONICOS,
        SAUDE,
        EDUCACAO
    }

    private final CategoryStats[] categories = new CategoryStats[Category.values().length];

    private final String[] transactionTypeNames = {"DEBITO", "CREDITO"};
    private final CategoryStats[] transactionTypes = new CategoryStats[transactionTypeNames.length];

    private final String[] paymentMethodNames = {"CARTAO", "BOLETO", "DINHEIRO", "CARTEIRA_DIGITAL"};
    private final CategoryStats[] paymentMethods = new CategoryStats[paymentMethodNames.length];

    private final String[] cityNames = {"Recife", "Olinda", "Jaboatao", "Paulista", "Caruaru"};
    private final CategoryStats[] cities = new CategoryStats[cityNames.length];

    public void accept(Transaction transaction) {
        double valor = transaction.getValue();

        this.count++;
        this.total += valor;

        if (valor > this.maximum) {
            this.maximum = valor;
        }
        if (valor < this.minimum) {
            this.minimum = valor;
        }

        if (DetectorTransacoesSuspeitas.ehSuspeita(transaction)) {
            this.suspicious++;
        }

        int categoryIndex = Category.valueOf(transaction.getCategory().toUpperCase()).ordinal();
        adicionarEstatistica(categories, categoryIndex, transaction);

        adicionarEstatistica(transactionTypes, encontrarIndice(transactionTypeNames, transaction.getType()), transaction);
        adicionarEstatistica(paymentMethods, encontrarIndice(paymentMethodNames, transaction.getMetodoPagamento()), transaction);
        adicionarEstatistica(cities, encontrarIndice(cityNames, transaction.getCity()), transaction);
    }

    public void merge(AnalysisResult other) {
        if (other == null) {
            return;
        }

        this.count += other.count;
        this.total += other.total;

        if (other.maximum > this.maximum) {
            this.maximum = other.maximum;
        }
        if (other.minimum < this.minimum) {
            this.minimum = other.minimum;
        }

        this.suspicious += other.suspicious;

        mesclarEstatisticas(this.categories, other.categories);
        mesclarEstatisticas(this.transactionTypes, other.transactionTypes);
        mesclarEstatisticas(this.paymentMethods, other.paymentMethods);
        mesclarEstatisticas(this.cities, other.cities);
    }

    public long count() {
        return count;
    }

    public double total() {
        return total;
    }

    public double average() {
        if (count == 0) {
            return 0.0;
        }
        return total / count;
    }

    public double maximum() {
        return count == 0 ? 0.0 : maximum;
    }

    public double minimum() {
        return count == 0 ? 0.0 : minimum;
    }

    public long suspicious() {
        return suspicious;
    }

    public CategoryStats[] categories() {
        return categories;
    }

    public CategoryStats[] transactionTypes() {
        return transactionTypes;
    }

    public String[] transactionTypeNames() {
        return transactionTypeNames;
    }

    public CategoryStats[] paymentMethods() {
        return paymentMethods;
    }

    public String[] paymentMethodNames() {
        return paymentMethodNames;
    }

    public CategoryStats[] cities() {
        return cities;
    }

    public String[] cityNames() {
        return cityNames;
    }

    private static void adicionarEstatistica(CategoryStats[] vetor, int indice, Transaction transacao) {
        if (indice < 0 || indice >= vetor.length) {
            return;
        }
        if (vetor[indice] == null) {
            vetor[indice] = new CategoryStats();
        }
        vetor[indice].add(transacao);
    }

    private static void mesclarEstatisticas(CategoryStats[] destino, CategoryStats[] origem) {
        for (int i = 0; i < destino.length; i++) {
            if (origem[i] == null) {
                continue;
            }
            if (destino[i] == null) {
                destino[i] = new CategoryStats();
            }
            destino[i].merge(origem[i]);
        }
    }

    private static int encontrarIndice(String[] nomes, String valor) {
        if (valor == null) {
            return 0;
        }
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equalsIgnoreCase(valor)) {
                return i;
            }
        }
        return 0;
    }
}