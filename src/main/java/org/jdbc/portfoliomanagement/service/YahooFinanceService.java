package org.jdbc.portfoliomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class YahooFinanceService {
    private final WebClient webclient;
    private final ObjectMapper objectMapper;

    public YahooFinanceService() {
        this.webclient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal getCurrentPrice(String symbol) {
        try {
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d", symbol);

            String response = webclient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            var root = objectMapper.readTree(response);
            var result = root.path("chart").path("result").get(0);

            JsonNode meta = result.path("meta");
            JsonNode regularMarketPriceNode = meta.path("regularMarketPrice");

            return new BigDecimal(regularMarketPriceNode.asDouble());

        } catch (Exception e) {
            System.err.println("Error fetching price from Yahoo Finance: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public List<HistoricalData> getHistoricalData(String symbol) {
        List<HistoricalData> historicalDataList = new ArrayList<>();

        try {
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1mo&interval=1d", symbol);

            String response = webclient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("chart").path("result").get(0);

            JsonNode timestamps = result.path("timestamp");
            JsonNode indicators = result.path("indicators").path("quote").get(0);
            JsonNode closes = indicators.path("close");

            for (int i = 0; i < timestamps.size(); i++) {
                long timestamp = timestamps.get(i).asLong();
                JsonNode closePriceNode = closes.get(i);

                if(closePriceNode == null || closePriceNode.isNull()) {
                    continue;
                }
                LocalDate date = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                BigDecimal price = new BigDecimal(closePriceNode.asDouble());
                historicalDataList.add(new HistoricalData(symbol, price, date));
            }
            historicalDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        } catch (Exception e) {
            System.err.println("Error fetching historical data from Yahoo Finance: " + e.getMessage());
            e.printStackTrace();
        }
        return historicalDataList;
    }

    public StockDetails getStockDetails(String symbol) {
        try {
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d", symbol);

            String response = webclient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("quoteResponse").path("result").get(0);

            JsonNode meta = result.path("meta");

            StockDetails details = new StockDetails();
            details.setSymbol(result.path("symbol").asText());
            details.setName(result.path("longName").asText());
            details.setPrice(new BigDecimal(result.path("regularMarketPrice").asDouble()));
            details.setOpen(new BigDecimal(result.path("regularMarketOpen").asDouble()));
            details.setDayHigh(new BigDecimal(result.path("regularMarketDayHigh").asDouble()));
            details.setDayLow(new BigDecimal(result.path("regularMarketDayLow").asDouble()));
            details.setPreviousClose(new BigDecimal(result.path("regularMarketPreviousClose").asDouble()));
            details.setCurrency(result.path("currency").asText());

            BigDecimal price = details.getPrice();
            BigDecimal previousClose = details.getPreviousClose();
            BigDecimal change = price.subtract(previousClose);
            BigDecimal changePercent = change.divide(previousClose, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
            details.setChange(change);
            details.setChangePercent(changePercent);

            return details;
        } catch (Exception e) {
            System.err.println("Error fetching stock details from Yahoo Finance: " + e.getMessage());
            return null;
        }
    }

    public static class HistoricalData {
        private String symbol;
        private BigDecimal price;
        private LocalDate date;

        public HistoricalData(String symbol, BigDecimal price, LocalDate date) {
            this.symbol = symbol;
            this.price = price;
            this.date = date;
        }

        public String getSymbol() {
            return symbol;
        }
        public BigDecimal getPrice() {
            return price;
        }
        public LocalDate getDate() {
            return date;
        }
    }

    public static class StockDetails {
        private String symbol;
        private String name;
        private BigDecimal price;
        private BigDecimal open;
        private BigDecimal dayHigh;
        private BigDecimal dayLow;
        private BigDecimal previousClose;
        private BigDecimal change;
        private BigDecimal changePercent;
        private Long volume;
        private String currency;

        public String getSymbol() {return symbol;}
        public String getName() {return name;}
        public BigDecimal getPrice() {return price;}
        public BigDecimal getOpen() {return open;}
        public BigDecimal getDayHigh() {return dayHigh;}
        public BigDecimal getDayLow() {return dayLow;}
        public BigDecimal getPreviousClose() {return previousClose;}
        public BigDecimal getChange() {return change;}
        public BigDecimal getChangePercent() {return changePercent;}
        public Long getVolume() {return volume;}
        public String getCurrency() {return currency;}
        public void setSymbol(String symbol) {this.symbol = symbol;}
        public void setName(String name) {this.name = name;}
        public void setPrice(BigDecimal price) {this.price = price;}
        public void setOpen(BigDecimal open) {this.open = open;}
        public void setDayHigh(BigDecimal dayHigh) {this.dayHigh = dayHigh;}
        public void setDayLow(BigDecimal dayLow) {this.dayLow = dayLow;}
        public void setPreviousClose(BigDecimal previousClose) {this.previousClose = previousClose;}
        public void setChange(BigDecimal change) {this.change = change;}
        public void setChangePercent(BigDecimal changePercent) {this.changePercent = changePercent;}
        public void setVolume(Long volume) {this.volume = volume;}
        public void setCurrency(String currency) {this.currency = currency;}
    }
}
