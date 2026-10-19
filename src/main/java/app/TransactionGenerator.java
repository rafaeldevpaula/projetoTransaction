package app;

import java.time.LocalDate;
import java.util.SplittableRandom;

public final class TransactionGenerator {
    private static final String[] CATEGORIES = {"Alimentacao", "Transporte", "Eletronicos", "Saude", "Educacao"};
    private static final String[] CITIES = {"Jacareí", "São José dos Campos", "Caçapava", "Taubaté", "Santa Branca"};
    private static final String[] ESTABLISHMENTS = {"Mercado Municipal", "UBER", "Drogaria Farma Conde", "Livraria Leitura", "Restaurante Giraffas"};
    private static final String[] PAYMENT_METHODS = {"CARTAO", "BOLETO", "DINHEIRO", "CARTEIRA_DIGITAL"};

    private TransactionGenerator() { }

    public static Transaction[] generate(int size, long seed) {
        Transaction[] transactions = new Transaction[size];
        SplittableRandom random = new SplittableRandom(seed);
        for (int index = 0; index < size; index++) {
            double value;
            switch (index % 4) {
                case 0 -> value = 20 + random.nextDouble() * 4980; // Variada Uniforme
                case 1 -> value = Math.max(10, 1800 + random.nextGaussian() * 1000); // Normal
                case 2 -> value = 10 + (-Math.log(1 - random.nextDouble()) * 2200); // Exponencial
                default -> value = 300 + random.nextDouble() * 900; // Variada Uniforme mais concentrada
            }
            if (index % 137 == 0) value += 12000;
            transactions[index] = new Transaction(index + 1L, "C" + (index % 10000),
                    LocalDate.of(2024, 1, 1).plusDays(index % 365L), value,
                    CATEGORIES[random.nextInt(CATEGORIES.length)], CITIES[random.nextInt(CITIES.length)], "SP",
                    index % 2 == 0 ? "DEBITO" : "CREDITO",
                    ESTABLISHMENTS[random.nextInt(ESTABLISHMENTS.length)],
                    PAYMENT_METHODS[random.nextInt(PAYMENT_METHODS.length)]);
        }
        return transactions;
    }
}