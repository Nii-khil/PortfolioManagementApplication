package org.jdbc.portfoliomanagement.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.jdbc.portfoliomanagement.entity.HistoricalPrice;
import org.jdbc.portfoliomanagement.repository.HistoricalPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HistoricalPriceService {

    @Autowired
    private HistoricalPriceRepository historicalPriceRepository;

    @Autowired
    private YahooFinanceService yahooFinanceService;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HistoricalPriceService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public List<HistoricalPrice> getHistoricalPrices(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) return Collections.emptyList();
        return historicalPriceRepository.findBySymbolOrderByPriceDateAsc(symbol.trim());
    }

    public List<HistoricalPrice> fetchAndStoreHistoricalData(String symbol, String assetType) {
        List<HistoricalPrice> prices = new ArrayList<>();

        try {
            String normalized = assetType == null ? "" : assetType.trim().toLowerCase();
            if (normalized.contains("stock")) {
                prices = fetchStockHistoricalData(symbol);
            } else if (normalized.contains("mutual") || normalized.contains("fund") || normalized.equals("mf")) {
                prices = fetchMutualFundHistoricalData(symbol);
            } else {
                // fallback: try stock first, then mutual fund
                prices = fetchStockHistoricalData(symbol);
                if (prices.isEmpty()) {
                    prices = fetchMutualFundHistoricalData(symbol);
                }
            }

            if (!prices.isEmpty()) {
                historicalPriceRepository.saveAll(prices);
            } else {
                System.err.println("No historical data found for symbol: " + symbol + " assetType: " + assetType);
            }
        } catch (Exception e) {
            System.err.println("Error fetching historical data for symbol: " + symbol + " assetType: " + assetType);
            e.printStackTrace();
        }
        return prices;
    }

    private List<HistoricalPrice> fetchStockHistoricalData(String symbol) {
        List<HistoricalPrice> prices = new ArrayList<>();

        try {
            List<YahooFinanceService.HistoricalData> historicalData = yahooFinanceService.getHistoricalData(symbol);
            for(YahooFinanceService.HistoricalData dataPoint : historicalData) {
                prices.add(new HistoricalPrice(dataPoint.getSymbol(), dataPoint.getPrice(), dataPoint.getDate()) );
            }
        } catch (Exception e) {
            System.err.println("Error fetching stock data for symbol: " + symbol);
            e.printStackTrace();
        }
        return prices;
    }

    private List<HistoricalPrice> fetchMutualFundHistoricalData(String schemeCode) {
        List<HistoricalPrice> prices = new ArrayList<>();

        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);

            String url = String.format(
                    "https://api.mfapi.in/mf/%s?startDate=%s&endDate=%s",
                    schemeCode, startDate.toString(), endDate.toString()
            );

            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                System.err.println("Empty response for mutual fund scheme code: " + schemeCode);
                // fallback to the general API without date range
                String fallBackUrl = String.format("https://api.mfapi.in/mf/%s", schemeCode);
                response = webClient.get()
                        .uri(fallBackUrl)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }

            if (response == null || response.isEmpty()) {
                System.err.println("Empty fallback response for mutual fund scheme code: " + schemeCode);
                return prices;
            }

            JsonNode jsonResponse = objectMapper.readTree(response);

            // mfapi returns top-level object with 'data' array
            JsonNode dataArray = jsonResponse.get("data");
            if (dataArray != null && dataArray.isArray()) {
                int count = 0;
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

                for (JsonNode dataPoint : dataArray) {
                    if (count >= 30) break;

                    JsonNode dateNode = dataPoint.get("date");
                    JsonNode navNode = dataPoint.get("nav");
                    if (dateNode == null || navNode == null) continue;

                    String dateStr = dateNode.asText();
                    String navStr = navNode.asText();

                    // Clean nav string: remove commas and handle NA/non-numeric
                    navStr = navStr.replaceAll(",", "").trim();
                    if (navStr.isEmpty() || "NA".equalsIgnoreCase(navStr)) continue;

                    LocalDate date;
                    try {
                        date = LocalDate.parse(dateStr, formatter);
                    } catch (Exception ex) {
                        // skip malformed date
                        continue;
                    }

                    BigDecimal nav;
                    try {
                        nav = new BigDecimal(navStr);
                    } catch (Exception ex) {
                        // skip non-numeric nav
                        continue;
                    }

                    HistoricalPrice historicalPrice = new HistoricalPrice(
                            schemeCode, nav, date
                    );
                    prices.add(historicalPrice);
                    count++;
                }

                prices.sort(Comparator.comparing(HistoricalPrice::getPriceDate));
            } else {
                System.err.println("No data array found for scheme code: " + schemeCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching mutual fund data for scheme code: " + schemeCode);
            e.printStackTrace();
        }

        return prices;
    }

}