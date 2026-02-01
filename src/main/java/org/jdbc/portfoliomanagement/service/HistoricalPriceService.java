package org.jdbc.portfoliomanagement.service;

import jakarta.persistence.Id;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "historical_prices")
public class HistoricalPriceService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private BigDecimal price;
    private LocalDate priceDate;

    public HistoricalPriceService() {
    }

    public HistoricalPriceService(String symbol, BigDecimal price, LocalDate priceDate) {
        this.symbol = symbol;
        this.price = price;
        this.priceDate = priceDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }
}