package app;

import java.time.LocalDate;

public record Transaction(long id, String client, LocalDate date, double value, String category,
                          String city, String state, String type, String estabelecimento,
                          String metodoPagamento) { }