package org.jdbc.portfoliomanagement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
public class PriceService {

    @Autowired
    private YahooFinanceService yahooFinanceService;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PriceService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal getCurrentPrice(String symbol, String assetType) {
        String type = assetType == null ? "" : assetType.toUpperCase();
        if ("STOCK".equals(type)) {
            return getStockPrice(symbol);
        } else if ("MUTUAL_FUND".equals(type)) {
            return getMutualFundPrice(symbol);
        } else {
            System.err.println("Unsupported asset type: " + assetType);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getStockPrice(String symbol) {
        return yahooFinanceService.getCurrentPrice(symbol);
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