package org.jdbc.portfoliomanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
public class PriceService {

    @Value("${alphavantage.api.key:demo}")
    private String alphaVantageApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PriceService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal getCurrentPrice(String symbol, String assetType) {
        if ("STOCK".equals(assetType)) {
            return getStockPrice(symbol);
        } else if ("MUTUAL_FUND".equals(assetType)) {
            return getMutualFundPrice(symbol);
        } else {
            System.err.println("Unsupported asset type: " + assetType);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getStockPrice(String symbol) {
        try {
            String url = String.format(
                    "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                    symbol, alphaVantageApiKey
            );

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode quote = root.path("Global Quote");

            if (quote.isMissingNode() || quote.path("05. price").isMissingNode()) {
                System.err.println("Alpha Vantage API limit or invalid symbol: " + symbol);
                return BigDecimal.ZERO;
            }

            String priceStr = quote.path("05. price").asText();
            return new BigDecimal(priceStr);

        } catch (Exception e) {
            System.err.println("Error fetching stock price for " + symbol + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getMutualFundPrice(String symbol) {
        try {
            String url = String.format("https://api.mfapi.in/mf/%s", symbol);

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isArray() && data.size() > 0) {
                JsonNode latest = data.get(0);
                String navStr = latest.path("nav").asText();
                return new BigDecimal(navStr);
            } else {
                System.err.println("No data from MFAPI for: " + symbol);
                return BigDecimal.ZERO;
            }

        } catch (Exception e) {
            System.err.println("Error fetching mutual fund price for " + symbol + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}