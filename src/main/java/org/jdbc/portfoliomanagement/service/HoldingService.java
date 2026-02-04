package org.jdbc.portfoliomanagement.service;

import org.jdbc.portfoliomanagement.entity.Holding;
import org.jdbc.portfoliomanagement.repository.HoldingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class HoldingService {
    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private YahooFinanceService yahooFinanceService;

    @Value("${currency.default.rate.usd-to-inr:89.0}")
    private Double defaultUsdToInrRate;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HoldingService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public List<Holding> getAllHoldings() {
        List<Holding> holdings = holdingRepository.findAll();
        holdings.forEach(this::addCalculatedFields);
        return holdings;
    }

    public Optional<Holding> getHoldingById(Long id) {
        Optional<Holding> holdings = holdingRepository.findById(id);
        holdings.ifPresent(this::addCalculatedFields);
        return holdings;
    }

    public Holding createHolding(Holding holding) {
        // Normalize asset type for consistency
        if (holding.getAssetType() != null) {
            holding.setAssetType(holding.getAssetType().toUpperCase());
        }

        Holding savedHolding = holdingRepository.save(holding);
        addCalculatedFields(savedHolding);
        return savedHolding;
    }

    public Optional<Holding> updateHolding(Long id, Holding holdingDetails) {
        return holdingRepository.findById(id)
                .map(holding -> {
                    // Normalize asset type from incoming details
                    if (holdingDetails.getAssetType() != null) {
                        holding.setAssetType(holdingDetails.getAssetType().toUpperCase());
                    } else {
                        holding.setAssetType(holdingDetails.getAssetType());
                    }

                    holding.setSymbol(holdingDetails.getSymbol());
                    holding.setQuantity(holdingDetails.getQuantity());
                    holding.setPurchasePrice(holdingDetails.getPurchasePrice());
                    holding.setPurchaseDate(holdingDetails.getPurchaseDate());
                    Holding updatedHolding = holdingRepository.save(holding);
                    addCalculatedFields(updatedHolding);
                    return updatedHolding;
                });
    }

    public boolean deleteHolding(Long id) {
        if(holdingRepository.existsById(id)) {
            holdingRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Holding> getHoldingsByAssetType(String assetType) {
        List<Holding> holdings = holdingRepository.findByAssetType(assetType);
        holdings.forEach(this::addCalculatedFields);
        return holdings;
    }

    private void addCalculatedFields(Holding holding) {
        // Normalize asset type to uppercase so downstream services (price/currency) work reliably
        if (holding.getAssetType() != null) {
            holding.setAssetType(holding.getAssetType().toUpperCase());
        }
        BigDecimal currentPrice = getCurrentPrice(holding.getSymbol(), holding.getAssetType());
        holding.setCurrentPrice(currentPrice);

        BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());
        holding.setCurrentValue(currentValue.setScale(2, RoundingMode.HALF_UP));

        BigDecimal purchaseValue = holding.getPurchasePrice().multiply(holding.getQuantity());
        BigDecimal profitLoss = currentValue.subtract(purchaseValue);
        holding.setProfitLoss(profitLoss.setScale(2, RoundingMode.HALF_UP));

        if(purchaseValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = profitLoss.divide(purchaseValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            holding.setProfitLossPercentage(percentage);
        } else {
            holding.setProfitLossPercentage(BigDecimal.ZERO);
        }

        String currency = getCurrencyCode(holding.getAssetType());
        String currencySymbol = getCurrencySymbol(holding.getAssetType());
        holding.setCurrency(currency);
        holding.setCurrencySymbol(currencySymbol);

        BigDecimal currentValueInr = convertToInr(currentValue, holding.getAssetType());
        BigDecimal purchaseValueInr = convertToInr(purchaseValue, holding.getAssetType());
        BigDecimal profitLossInr = currentValueInr.subtract(purchaseValueInr);

        holding.setCurrentValueInr(currentValueInr.setScale(2, RoundingMode.HALF_UP));
        holding.setProfitLossInr(profitLossInr.setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal getCurrentPrice(String symbol, String assetType) {
        String type = assetType == null ? "" : assetType.toUpperCase();
        if ("STOCK".equals(type)) {
            return getStockPrice(symbol);
        } else if ("MUTUAL_FUND".equals(type) || "MUTUAL-FUND".equals(type) || "MF".equals(type)) {
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

            if (response == null || response.isEmpty()) {
                System.err.println("No data from MFAPI for: " + symbol);
                return BigDecimal.ZERO;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");

            if (data.isArray() && data.size() > 0) {
                JsonNode latest = data.get(0);
                String navStr = latest.path("nav").asText();
                try {
                    return new BigDecimal(navStr.replaceAll(",", "").trim());
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            } else {
                System.err.println("No data from MFAPI for: " + symbol);
                return BigDecimal.ZERO;
            }

        } catch (Exception e) {
            System.err.println("Error fetching mutual fund price for " + symbol + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
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

    public Map<String, Object> getPortfolioSummary() {
        List<Holding> holdings = getAllHoldings();

        Map<String, Object> summary = new HashMap<>();

        if(holdings.isEmpty()) {
            summary.put("totalValue", BigDecimal.ZERO);
            summary.put("totalInvestment", BigDecimal.ZERO);
            summary.put("totalProfitLoss", BigDecimal.ZERO);
            summary.put("totalProfitLossPercentage", BigDecimal.ZERO);
            summary.put("totalHoldings", 0);
            summary.put("compositionByAssetType", new HashMap<>());
            summary.put("compositionByCategory", new HashMap<>());
            summary.put("currency", "INR");
            summary.put("currencySymbol", "₹");
            summary.put("exchangeRate", getUsdToInrRate());
            return summary;
        }

        BigDecimal totalCurrentValueInr = BigDecimal.ZERO;
        BigDecimal totalInvestmentInr = BigDecimal.ZERO;
        Map<String, BigDecimal> assetCompositionMap = new HashMap<>();
        Map<String, BigDecimal> categoryCompositionMap = new HashMap<>();

        for(Holding holding : holdings) {
            totalCurrentValueInr = totalCurrentValueInr.add(holding.getCurrentValueInr());

            BigDecimal purchaseValue = holding.getPurchasePrice().multiply(holding.getQuantity());
            BigDecimal purchaseValueInr = convertToInr(purchaseValue, holding.getAssetType());
            totalInvestmentInr = totalInvestmentInr.add(purchaseValueInr);

            String assetType = holding.getAssetType();
            BigDecimal currentAssetValue = assetCompositionMap.getOrDefault(assetType, BigDecimal.ZERO);
            assetCompositionMap.put(assetType, currentAssetValue.add(holding.getCurrentValueInr()));

            if(holding.getCategory() != null && !holding.getCategory().isEmpty()) {
                String category = holding.getCategory();
                BigDecimal currentCategoryValue = categoryCompositionMap.getOrDefault(category, BigDecimal.ZERO);
                categoryCompositionMap.put(category, currentCategoryValue.add(holding.getCurrentValueInr()));
            }
        }

        BigDecimal totalProfitLoss = totalCurrentValueInr.subtract(totalInvestmentInr);

        BigDecimal profitLossPercentage = BigDecimal.ZERO;
        if(totalInvestmentInr.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercentage = totalProfitLoss.divide(totalInvestmentInr, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        summary.put("totalValue", totalCurrentValueInr.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalInvestment", totalInvestmentInr.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalProfitLoss", totalProfitLoss.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalProfitLossPercentage", profitLossPercentage.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalHoldings", holdings.size());
        summary.put("currency", "INR");
        summary.put("currencySymbol", "₹");
        summary.put("exchangeRate", getUsdToInrRate());

        Map<String, BigDecimal> roundedAssetComposition = new HashMap<>();
        assetCompositionMap.forEach((key, value) ->
                roundedAssetComposition.put(key, value.setScale(2, RoundingMode.HALF_UP))
        );
        summary.put("compositionByAssetType", roundedAssetComposition);

        Map<String, BigDecimal> roundedCategoryComposition = new HashMap<>();
        categoryCompositionMap.forEach((key, value) ->
                roundedCategoryComposition.put(key, value.setScale(2, RoundingMode.HALF_UP))
        );
        summary.put("compositionByCategory", roundedCategoryComposition);

        return summary;
    }

    public Holding getBestPerformer() {
        List<Holding> holdings = getAllHoldings();
        return holdings.stream()
                .max((h1, h2) -> h1.getProfitLossPercentage().compareTo(h2.getProfitLossPercentage())).orElse(null);
    }

    public Holding getWorstPerformer() {
        List<Holding> holdings = getAllHoldings();
        return holdings.stream()
                .min((h1, h2) -> h1.getProfitLossPercentage().compareTo(h2.getProfitLossPercentage())).orElse(null);
    }

    public Map<String, Object> getDiversificationSuggestions() {
        List<Holding> holdings = getAllHoldings();
        Map<String, Object> suggestions = new HashMap<>();
        List<String> recommendationsList = new ArrayList<>();

        if(holdings.isEmpty()) {
            suggestions.put("needsDiversification", false);
            suggestions.put("recommendations", recommendationsList);
            suggestions.put("riskLevel", "N/A");
            return suggestions;
        }

        BigDecimal tempTotalStockValue = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryValues = new HashMap<>();

        for(Holding holding : holdings) {
            if("STOCK".equals(holding.getAssetType()) && holding.getCategory() != null) {
                tempTotalStockValue = tempTotalStockValue.add(holding.getCurrentValue());
                String category = holding.getCategory();
                BigDecimal currentValue = categoryValues.getOrDefault(category, BigDecimal.ZERO);
                categoryValues.put(category, currentValue.add(holding.getCurrentValue()));
            }
        }

        final BigDecimal totalStockValue = tempTotalStockValue;

        if(totalStockValue.compareTo(BigDecimal.ZERO) == 0) {
            suggestions.put("needsDiversification", false);
            suggestions.put("recommendations", recommendationsList);
            suggestions.put("riskLevel", "Low");
            return suggestions;
        }

        Map<String, BigDecimal> categoryPercentages = new HashMap<>();
        categoryValues.forEach((category, value) -> {
            BigDecimal percentage = value.divide(totalStockValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            categoryPercentages.put(category, percentage);
        });

        boolean needsDiversification = false;
        final String[] riskLevel = {"Moderate"};

        for(Map.Entry<String, BigDecimal> entry : categoryPercentages.entrySet()) {
            String category = entry.getKey();
            BigDecimal percentage = entry.getValue();

            if(percentage.compareTo(new BigDecimal(40)) > 0) {
                needsDiversification = true;
                riskLevel[0] = "High";
                recommendationsList.add(String.format("High concentration in %s (%.2f%%). Consider diversifying into other sectors.", category, percentage.doubleValue()));
            } else if (percentage.compareTo(new BigDecimal(30)) > 0) {
                recommendationsList.add(String.format("Moderate concentration in %s (%.2f%%). Monitor and consider diversification if it increases.", category, percentage.doubleValue()));
            }
        }

        Set<String> importantCategories = new HashSet<>(Arrays.asList("Technology", "Healthcare", "Finance", "Consumer Goods", "Energy"));
        Set<String> missingCategories = new HashSet<>(importantCategories);
        missingCategories.removeAll(categoryValues.keySet());

        if(!missingCategories.isEmpty() && categoryValues.size() < 3) {
            needsDiversification = true;
            for(String missingCategory : missingCategories) {
                recommendationsList.add(String.format("Consider adding holdings in the %s sector for better diversification.", missingCategory));
            }
        }

        if(categoryValues.size() == 1) {
            riskLevel[0] = "Very High";
            needsDiversification = true;
            recommendationsList.add("Portfolio is concentrated in a single category. Consider diversifying into multiple sectors.");
        } else if (categoryValues.size() == 2) {
            riskLevel[0] = "High";
            recommendationsList.add("Portfolio has limited category diversification. Consider adding more sectors.");
        } else if (categoryValues.size() >= 5) {
            riskLevel[0] = "Low";
            recommendationsList.add("Good diversification across multiple sectors!");
        }

        Map<String, Object> summary = getPortfolioSummary();
        Map<String, BigDecimal> assetComposition = (Map<String, BigDecimal>) summary.get("compositionByAssetType");
        BigDecimal totalValue = (BigDecimal) summary.get("totalValue");

        if(assetComposition.containsKey("STOCK")) {
            BigDecimal stockPercentage = assetComposition.get("STOCK")
                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if(stockPercentage.compareTo(new BigDecimal("80")) > 0) {
                recommendationsList.add(String.format("High allocation to stocks (%.2f%%). Consider balancing with other asset types.", stockPercentage.doubleValue()));
            } else if (stockPercentage.compareTo(new BigDecimal("90")) > 0) {
                riskLevel[0] = "Very High";
                needsDiversification = true;
                recommendationsList.add(String.format("Very high allocation to stocks (%.2f%%). This increases risk significantly.", stockPercentage.doubleValue()));
            }
        }
        if(recommendationsList.isEmpty()) {
            recommendationsList.add("Your portfolio is well diversified!");
        }

        suggestions.put("needsDiversification", needsDiversification);
        suggestions.put("recommendations", recommendationsList);
        suggestions.put("riskLevel", riskLevel[0]);
        suggestions.put("categoryBreakdown", categoryPercentages);

        return suggestions;
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
                details.put("price", q.has("regularMarketPrice") ? q.path("regularMarketPrice").asText() : "");
                details.put("open", q.has("regularMarketOpen") ? q.path("regularMarketOpen").asText() : "");
                details.put("high", q.has("regularMarketDayHigh") ? q.path("regularMarketDayHigh").asText() : "");
                details.put("low", q.has("regularMarketDayLow") ? q.path("regularMarketDayLow") .asText() : "");
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
                int count = 0;
                for (JsonNode fundNode : jsonResponse) {
                    if (count >= 20) break;

                    Map<String, String> fund = new HashMap<>();
                    fund.put("schemeCode", fundNode.path("schemeCode").asText());
                    fund.put("schemeName", fundNode.path("schemeName").asText());
                    funds.add(fund);
                    count++;
                }
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
