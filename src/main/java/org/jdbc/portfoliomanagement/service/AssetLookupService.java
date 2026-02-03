package org.jdbc.portfoliomanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssetLookupService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AssetLookupService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> searchStocks(String keywords) {
        Map<String, Object> result = new HashMap<>();

        try {
            String url = String.format(
                    "https://query2.finance.yahoo.com/v1/finance/search?q=%s&quotesCount=15&newsCount=0",
                    keywords
            );

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                result.put("matches", new ArrayList<>());
                return result;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);

            JsonNode quotes = jsonResponse.get("quotes");
            if (quotes != null && quotes.isArray()) {
                List<Map<String, String>> matches = new ArrayList<>();

                for (JsonNode q : quotes) {
                    String symbol = q.path("symbol").asText();
                    String name = q.has("shortname") ? q.path("shortname").asText() : q.path("longname").asText();
                    String exch = q.path("exchange").asText();
                    String currency = q.path("currency").asText();

                    // Only include items that look like equities (quoteType may be EQUITY)
                    Map<String, String> stockInfo = new HashMap<>();
                    stockInfo.put("symbol", symbol != null ? symbol : "");
                    stockInfo.put("name", name != null ? name : "");
                    stockInfo.put("exch", exch != null ? exch : "");
                    stockInfo.put("currency", currency != null ? currency : "");
                    matches.add(stockInfo);

                    if (matches.size() >= 15) break;
                }

                result.put("matches", matches);
            } else {
                result.put("matches", new ArrayList<>());
            }

        } catch (Exception e) {
            result.put("error", "Error searching stocks: " + e.getMessage());
            result.put("matches", new ArrayList<>());
        }

        return result;
    }

    public Map<String, Object> getStockDetails(String symbol) {
        Map<String, Object> details = new HashMap<>();

        try {
            String quoteUrl = String.format(
                    "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s",
                    symbol
            );

            String response = webClient.get()
                    .uri(quoteUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                details.put("error", "No data found for symbol: " + symbol);
                return details;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode quoteResponse = jsonResponse.path("quoteResponse");
            JsonNode results = quoteResponse.path("result");

            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode q = results.get(0);

                details.put("symbol", q.path("symbol").asText());
                // map Yahoo fields to the existing frontend-friendly keys
                details.put("price", q.has("regularMarketPrice") ? q.path("regularMarketPrice").asText() : "");
                details.put("open", q.has("regularMarketOpen") ? q.path("regularMarketOpen").asText() : "");
                details.put("high", q.has("regularMarketDayHigh") ? q.path("regularMarketDayHigh").asText() : "");
                details.put("low", q.has("regularMarketDayLow") ? q.path("regularMarketDayLow").asText() : "");
                details.put("volume", q.has("regularMarketVolume") ? q.path("regularMarketVolume").asText() : "");
                details.put("latestTradingDay", q.has("regularMarketTime") ? q.path("regularMarketTime").asText() : "");
                details.put("previousClose", q.has("regularMarketPreviousClose") ? q.path("regularMarketPreviousClose").asText() : "");
                details.put("change", q.has("regularMarketChange") ? q.path("regularMarketChange").asText() : "");
                details.put("changePercent", q.has("regularMarketChangePercent") ? q.path("regularMarketChangePercent").asText() : "");

            } else {
                details.put("error", "No data found for symbol: " + symbol);
            }

        } catch (Exception e) {
            details.put("error", "Error fetching stock details: " + e.getMessage());
        }

        return details;
    }

    public Map<String, Object> searchMutualFunds(String keywords) {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("Searching mutual funds for: " + keywords);
            String url = String.format("https://api.mfapi.in/mf/search?q=%s", keywords);

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                System.err.println("Empty response from MFAPI search");
                result.put("results", new ArrayList<>());
                return result;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);
            List<Map<String, String>> funds = new ArrayList<>();

            if (jsonResponse.isArray()) {
                System.out.println("Found " + jsonResponse.size() + " mutual fund results");

                int count = 0;
                for (JsonNode fundNode : jsonResponse) {
                    if (count >= 20) break;

                    Map<String, String> fund = new HashMap<>();
                    fund.put("schemeCode", fundNode.path("schemeCode").asText());
                    fund.put("schemeName", fundNode.path("schemeName").asText());
                    funds.add(fund);
                    count++;
                }

                System.out.println("Returning " + funds.size() + " mutual fund results");
            }

            result.put("results", funds);

        } catch (Exception e) {
            System.err.println("Error searching mutual funds: " + e.getMessage());
            result.put("error", "Error searching mutual funds: " + e.getMessage());
            result.put("results", new ArrayList<>());
        }

        return result;
    }

    public Map<String, Object> getMutualFundDetails(String schemeCode) {
        Map<String, Object> details = new HashMap<>();

        try {
            System.out.println("Fetching mutual fund details for scheme: " + schemeCode);
            String url = String.format("https://api.mfapi.in/mf/%s/latest", schemeCode);

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                System.err.println("Empty response from MFAPI for scheme: " + schemeCode);
                details.put("error", "No data found for scheme: " + schemeCode);
                return details;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("meta")) {
                JsonNode meta = jsonResponse.get("meta");
                details.put("schemeCode", meta.path("scheme_code").asText());
                details.put("schemeName", meta.path("scheme_name").asText());
                details.put("fundHouse", meta.path("fund_house").asText());
                details.put("schemeType", meta.path("scheme_type").asText());
                details.put("schemeCategory", meta.path("scheme_category").asText());
            }

            if (jsonResponse.has("data") && jsonResponse.get("data").isArray() && jsonResponse.get("data").size() > 0) {
                JsonNode latestData = jsonResponse.get("data").get(0);
                details.put("latestNAV", latestData.path("nav").asText());
                details.put("navDate", latestData.path("date").asText());
            }

            System.out.println("Successfully fetched mutual fund details for " + schemeCode);

        } catch (Exception e) {
            System.err.println("Error fetching mutual fund details: " + e.getMessage());
            details.put("error", "Error fetching mutual fund details: " + e.getMessage());
        }

        return details;
    }
}
