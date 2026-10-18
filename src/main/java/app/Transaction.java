package app;

import java.time.LocalDate;

public class Transaction {
    private final long id;
    private final String client;
    private final LocalDate date;
    private final double value;
    private final String category;
    private final String city;
    private final String state;
    private final String type;
    private final String estabelecimento;
    private final String metodoPagamento;

    public Transaction(long id, String client, LocalDate date, double value, String category,
                       String city, String state, String type, String estabelecimento,
                       String metodoPagamento) {
        this.id = id;
        this.client = client;
        this.date = date;
        this.value = value;
        this.category = category;
        this.city = city;
        this.state = state;
        this.type = type;
        this.estabelecimento = estabelecimento;
        this.metodoPagamento = metodoPagamento;
    }

    public long getId() {
        return id;
    }

    public String getClient() {
        return client;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }

    public String getCategory() {
        return category;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public String getEstabelecimento() {
        return estabelecimento;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public long id() {
        return id;
    }

    public String client() {
        return client;
    }

    public LocalDate date() {
        return date;
    }

    public double value() {
        return value;
    }

    public String category() {
        return category;
    }

    public String city() {
        return city;
    }

    public String state() {
        return state;
    }

    public String type() {
        return type;
    }

    public String estabelecimento() {
        return estabelecimento;
    }

    public String metodoPagamento() {
        return metodoPagamento;
    }
}