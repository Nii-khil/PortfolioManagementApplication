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

    private final HistoricalPriceRepository historicalPriceRepository;

    private final String alphaVantageApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public HistoricalPriceService(HistoricalPriceRepository historicalPriceRepository,
                                  @Value("${alphavantage.api.key:demo}") String alphaVantageApiKey) {
        this.historicalPriceRepository = historicalPriceRepository;
        // Instantiate a local ObjectMapper to avoid requiring a container-managed bean
        this.objectMapper = new ObjectMapper();
        this.alphaVantageApiKey = alphaVantageApiKey;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30))
                                .addHandlerLast(new WriteTimeoutHandler(30)));

        this.webClient = WebClient.builder()
                .baseUrl("https://www.alphavantage.co")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
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
            String url = String.format(
                    "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                    symbol, alphaVantageApiKey
            );
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (response == null || response.isEmpty()) return prices;

            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("Note") || jsonResponse.has("Information")) {
                System.err.println("API limit reached");
                return prices;
            }

            JsonNode timeSeries = jsonResponse.get("Time Series (Daily)");
            if (timeSeries != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
                int count = 0;

                while (fields.hasNext() && count < 30) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String dateStr = entry.getKey();
                    JsonNode dailyData = entry.getValue();

                    LocalDate date = LocalDate.parse(dateStr);
                    double closePrice = dailyData.get("4. close").asDouble();

                    HistoricalPrice historicalPrice = new HistoricalPrice(
                            symbol,
                            BigDecimal.valueOf(closePrice),
                            date
                    );
                    prices.add(historicalPrice);
                    count++;
                }

                prices.sort(Comparator.comparing(HistoricalPrice::getPriceDate));
            } else {
                System.err.println("No time series data found for symbol: " + symbol);
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