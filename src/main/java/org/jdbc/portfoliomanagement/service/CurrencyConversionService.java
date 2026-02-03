package org.jdbc.portfoliomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyConversionService {
    @Value("${currency.default.rate.usd-to-inr:89.0}")
    private Double defaultUsdToInrRate;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CurrencyConversionService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public BigDecimal convertUsdToInr(BigDecimal usdAmount) {
        if(usdAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = getUsdToInrRate();
        return usdAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getUsdToInrRate() {
        BigDecimal liveRate = fetchLiveUsdToInrRate();
        if (liveRate != null && liveRate.compareTo(BigDecimal.ZERO) > 0) {
            return liveRate;
        } else {
            return BigDecimal.valueOf(defaultUsdToInrRate);
        }
    }

    private BigDecimal fetchLiveUsdToInrRate() {
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/USD";

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if(response != null && !response.isEmpty()) {
                JsonNode jsonResponse = objectMapper.readTree(response);
                JsonNode rates = jsonResponse.get("rates");
                if(rates != null && rates.has("INR")) {
                    return new BigDecimal(rates.get("INR").asText());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching live USD to INR rate: " + e.getMessage());
        }
        return null;
    }

    public String getCurrencySymbol(String assetType) {
        if("STOCK".equals(assetType)) {
            return "$";
        } else if ("MUTUAL_FUND".equals(assetType)) {
            return "₹";
        } else {
            return "";
        }
    }

    public String getCurrencyCode(String assetType) {
        if("STOCK".equals(assetType)) {
            return "USD";
        } else if ("MUTUAL_FUND".equals(assetType)) {
            return "INR";
        } else {
            return "";
        }
    }

    public BigDecimal convertToInr(BigDecimal amount, String assetType) {
        if(amount == null) {
            return BigDecimal.ZERO;
        }

        if("STOCK".equals(assetType)) {
            return convertUsdToInr(amount);
        } else {
            return amount;
        }
    }
}
